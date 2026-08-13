package nfcmgg.plugin.worksheet

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.codehaus.groovy.control.CompilationFailedException

import nfcmgg.plugin.utils.SafeGroovy
import nfcmgg.plugin.samplesheets.OutputEntry
import nfcmgg.plugin.samplesheets.SamplesheetCreator

/**
 * A class used to define which samplesheets should be generated and what their content is
 */
@CompileStatic
@Slf4j
class WorksheetSamplesheets {

    private static final Pattern DATA_FIELD_PATTERN = Pattern.compile(/data\.([A-Za-z_][A-Za-z0-9_]*)/)

    final SamplesheetCreator creator = new SamplesheetCreator()

    /**
     * Samplesheet definitions in declaration order
     */
    final List<Samplesheet> samplesheets

    WorksheetSamplesheets(List<Map> samplesheets, Set<String> dataFields) {
        if (samplesheets == null || samplesheets.isEmpty()) {
            log.error('Worksheet samplesheets is missing or empty')
        }
        final List<Samplesheet> parsed = []
        samplesheets.each { Map entry ->
            parsed.add(new Samplesheet(entry, dataFields))
        }
        this.samplesheets = parsed.asImmutable()
    }

    void publishSamplesheets(Map<String, OutputEntry> entries, Path location) {
        samplesheets.each { Samplesheet samplesheet ->
            samplesheet.publish(entries, location, creator)
        }
    }

    /**
     * A single samplesheet definition
     */
    @CompileStatic
    static class Samplesheet {

        final String name
        final String includeFunc
        final String filterFunc
        final List<Field> fields

        Samplesheet(Map entry, Set<String> dataFields) {
            if (entry.name == null) {
                log.error('Samplesheet entry is missing the required \'name\' field')
            }
            name = entry.name as String
            includeFunc = entry.include_func != null ? entry.include_func as String : null
            filterFunc = entry.filter_func != null ? entry.filter_func as String : null
            if (includeFunc != null) {
                validateFunc(name, 'include_func', includeFunc, dataFields)
            }
            if (filterFunc != null) {
                validateFunc(name, 'filter_func', filterFunc, dataFields)
            }
            if (entry.fields == null) {
                log.error("Samplesheet '${name}' is missing the required 'fields' field")
            }
            final Map<String, Object> fieldsMap = (
                entry.fields != null ? entry.fields as Map : [:]
            ) as Map<String, Object>
            final List<Field> parsed = fieldsMap.collect { String key, Object value ->
                return new Field(key.toString(), value)
            }
            fields = parsed.asImmutable()
        }

        void publish(Map<String, OutputEntry> entries, Path location, SamplesheetCreator creator) {
            if (!name || !fields) {
                return
            }
            Map<String, OutputEntry> filteredEntries = entries
            if (filterFunc != null) {
                filteredEntries = entries.findAll { String key, OutputEntry entry ->
                    Binding binding = new Binding([data: entry])
                    GroovyShell shell = SafeGroovy.shell(binding)
                    shell.evaluate(filterFunc) as Boolean
                }
            }
            List<OutputEntry> passedEntries = []
            List<OutputEntry> failedEntries = []
            if (includeFunc != null) {
                filteredEntries.each { String key, OutputEntry entry ->
                    Binding binding = new Binding([data: entry])
                    GroovyShell shell = SafeGroovy.shell(binding)
                    if (shell.evaluate(includeFunc) as Boolean) {
                        passedEntries.add(entry)
                    } else {
                        failedEntries.add(entry)
                    }
                }
            } else {
                passedEntries = filteredEntries.values().toList()
            }
            Path passedSamplesheet = location.resolve(name)
            Path failedSamplesheet = location.resolve(
                (passedSamplesheet.baseName + '_failed' + passedSamplesheet.extension) as String
            )
            creator.dump(subsetEntries(passedEntries), passedSamplesheet)
            creator.dump(subsetEntries(failedEntries), failedSamplesheet)
        }

        private static void validateFunc(String name, String funcName, String func, Set<String> dataFields) {
            try {
                new GroovyShell().parse(func)
            } catch (CompilationFailedException e) {
                log.error("Invalid Groovy expression in samplesheet '${name}' ${funcName}: ${e.message}")
            }
            final Matcher matcher = DATA_FIELD_PATTERN.matcher(func)
            while (matcher.find()) {
                final String referenced = matcher.group(1)
                if (!dataFields.contains(referenced)) {
                    log.error(
                        "Samplesheet '${name}' ${funcName} references unknown data field 'data.${referenced}'. " +
                        "Available data fields: ${dataFields.sort().join(', ')}"
                    )
                }
            }
        }

    }

    private static List<OutputEntry> subsetEntries(List<OutputEntry> entries) {
        List<OutputEntry> subset = []
        entries.each { OutputEntry entry ->
            Map<String, Object> newStructure = [:]
            fields.each { Field field ->
                newStructure[field.key] = entry.get(field.source)
            }
            subset.add(new OutputEntry(newStructure))
        }
        return subset
    }

    /**
     * A single samplesheet field mapping
     */
    @CompileStatic
    static class Field {

        final String key
        final String source

        Field(String key, Object fieldValue) {
            this.key = key
            // If the value is omitted, the data field name matches the key
            source = fieldValue != null ? fieldValue as String : key
        }

    }

}
