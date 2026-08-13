package nfcmgg.plugin.worksheet

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.codehaus.groovy.control.CompilationFailedException

import nfcmgg.plugin.utils.SafeGroovy

/**
 * A class used to define which samplesheets should be generated and what their content is
 */
@CompileStatic
@Slf4j
class WorksheetSamplesheets {

    final SamplesheetCreator creator = new SamplesheetCreator()

    private static final Pattern DATA_FIELD_PATTERN = Pattern.compile(/data\.([A-Za-z_][A-Za-z0-9_]*)/)

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
            samplesheet.publish(entries, location)
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
            final Map fieldsMap = (entry.fields != null ? entry.fields as Map : [:]) as Map
            final List<Field> parsed = []
            fieldsMap.each { String key, Object value ->
                parsed.add(new Field(key, value))
            }
            fields = parsed.asImmutable()
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

        void publish(Map<String, OutputEntry> entries, Path location) {
            if (!name || !fields) {
                return
            }
            List<OutputEntry> filteredEntries = entries
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
                passedEntries = filteredEntries.values()
            }
            Path passedSamplesheet = location.resolve(name)
            Path failedSamplesheet = location.resolve(
                passedSamplesheet.basename + '_failed' + passedSamplesheet.extension
            )
            creator.dump(subsetEntries(passedEntries), passedSamplesheet)
            creator.dump(subsetEntries(failedEntries), failedSamplesheet)
        }

    }

    private static List<OutputEntry> subsetEntries(List<OutputEntry> entries) {
        List<OutputEntry> subset = []
        entries.each { OutputEntry entry ->
            Map newStructure = [:]
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
