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
            final WorksheetErrors errors = new WorksheetErrors()
            errors.error('Worksheet samplesheets is missing or empty')
            errors.throwIfAny('Invalid worksheet samplesheets')
        }
        final List<Samplesheet> parsed = []
        samplesheets.each { rawEntry ->
            try {
                final Map entry = rawEntry as Map
                final WorksheetErrors entryErrors = new WorksheetErrors()
                final Samplesheet sheet = new Samplesheet(entry, dataFields, entryErrors)
                if (entryErrors.hasErrors()) {
                    final String label = entry?.name ?: '(unnamed)'
                    log.error("Skipping samplesheet '${label}' due to validation errors")
                } else {
                    parsed.add(sheet)
                }
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                log.error("Skipping samplesheet entry due to an error: ${e.message}")
            }
        }
        if (parsed.isEmpty()) {
            log.error('No valid samplesheet entries remain after validation')
        }
        this.samplesheets = parsed.asImmutable()
    }

    void publishSamplesheets(Map<String, OutputEntry> entries, Path location, Map<String, Object> params) {
        samplesheets.each { Samplesheet samplesheet ->
            try {
                log.info("Publishing samplesheet '${samplesheet.name}'")
                samplesheet.publish(entries, location, creator, params)
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                log.error("Skipping samplesheet '${samplesheet.name}' due to an error: ${e.message}")
            }
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

        Samplesheet(Map entry, Set<String> dataFields, WorksheetErrors errors) {
            if (entry.name == null) {
                errors.error('Samplesheet entry is missing the required \'name\' field')
            }
            name = entry.name as String
            includeFunc = entry.include_func != null ? entry.include_func as String : null
            filterFunc = entry.filter_func != null ? entry.filter_func as String : null
            if (includeFunc != null) {
                validateFunc(name, 'include_func', includeFunc, dataFields, errors)
            }
            if (filterFunc != null) {
                validateFunc(name, 'filter_func', filterFunc, dataFields, errors)
            }
            if (entry.fields == null) {
                errors.error("Samplesheet '${name}' is missing the required 'fields' field")
            }
            final Map<String, Map> fieldsMap = (
                entry.fields != null ? entry.fields as Map : [:]
            ) as Map<String, Map>
            final List<Field> parsed = fieldsMap.collect { String key, Map value ->
                return new Field(key.toString(), value, errors)
            }
            fields = parsed.asImmutable()
        }

        void publish(
            Map<String, OutputEntry> entries,
            Path location,
            SamplesheetCreator creator,
            Map<String, Object> params
        ) {
            if (!name || !fields) {
                return
            }
            List<OutputEntry> sortedEntries = entries.entrySet()
                .sort { a, b -> a.key <=> b.key }
                *.value
            List<OutputEntry> filteredEntries = sortedEntries
            if (includeFunc != null) {
                filteredEntries = sortedEntries.findAll { OutputEntry entry ->
                    Binding binding = new Binding([data: entry, params: params])
                    GroovyShell shell = SafeGroovy.shell(binding)
                    shell.evaluate(includeFunc) as Boolean
                }
            }

            List<OutputEntry> transposedEntries = filteredEntries.collectMany { OutputEntry entry ->
                List<Map<String, Object>> multiValues = []
                Map<String, Object> values = entry.values
                    .collectEntries { String key, Object value ->
                        Object realValue = value
                        if (value in List) {
                            List<Object> listValue = value as List<Object>
                            if (listValue.size() > 1) {
                                listValue.sort()
                                listValue.eachWithIndex { Object listValueItem, int index ->
                                    /* groovylint-disable-next-line NestedBlockDepth */
                                    if (multiValues.size() <= index) {
                                        multiValues.add([:])
                                    }
                                    multiValues[index] += [(key.toString()): listValueItem]
                                }
                                return null
                            }
                            realValue = listValue.first()
                        }
                        return [key, realValue]
                    }
                List<OutputEntry> finalEntries = []
                if (multiValues.size() > 0) {
                    multiValues.each { Map<String, Object> multiValue ->
                        finalEntries.add(new OutputEntry(values + multiValue))
                    }
                } else {
                    finalEntries.add(new OutputEntry(values))
                }
                return finalEntries
            } as List<OutputEntry>

            Path passedSamplesheet = location.resolve(name)

            List<OutputEntry> passedEntries = []
            List<OutputEntry> failedEntries = []
            if (filterFunc != null) {
                transposedEntries.each { OutputEntry entry ->
                    Binding binding = new Binding([data: entry, params: params])
                    GroovyShell shell = SafeGroovy.shell(binding)
                    if (shell.evaluate(filterFunc) as Boolean) {
                        passedEntries.add(entry)
                    } else {
                        failedEntries.add(entry)
                    }
                }
                Path failedSamplesheet = location.resolve(
                    (passedSamplesheet.baseName + '_failed.' + passedSamplesheet.extension) as String
                )
                creator.dump(subsetEntries(failedEntries), failedSamplesheet)
            } else {
                passedEntries = transposedEntries
            }
            creator.dump(subsetEntries(passedEntries), passedSamplesheet)
        }

        private List<OutputEntry> subsetEntries(List<OutputEntry> entries) {
            List<OutputEntry> subset = []
            entries.each { OutputEntry entry ->
                Map<String, Object> newStructure = [:]
                fields.each { Field field ->
                    if (entry.get(field.source) != null) {
                        newStructure[field.key] = field.convert(entry)
                    }
                }
                subset.add(new OutputEntry(newStructure))
            }
            return subset
        }

        private void validateFunc(
            String name, String funcName, String func, Set<String> dataFields, WorksheetErrors errors
        ) {
            try {
                SafeGroovy.parse(func)
            } catch (CompilationFailedException e) {
                errors.error("Invalid Groovy expression in samplesheet '${name}' ${funcName}: ${e.message}")
            }
            final Matcher matcher = DATA_FIELD_PATTERN.matcher(func)
            while (matcher.find()) {
                final String referenced = matcher.group(1)
                if (!dataFields.contains(referenced)) {
                    errors.error(
                        "Samplesheet '${name}' ${funcName} references unknown data field 'data.${referenced}'. " +
                        "Available data fields: ${dataFields.sort().join(', ')}"
                    )
                }
            }
        }

    }

    /**
     * A single samplesheet field mapping
     */
    @CompileStatic
    static class Field {

        final String key
        final String source
        final String type

        Field(String key, Map<String, Object> fieldValue, WorksheetErrors errors) {
            this.key = key
            // If the value is omitted, the data field name matches the key
            source = fieldValue?.get('source')?.toString() ?: key
            type = fieldValue?.get('type')?.toString() ?: ''
            if (type != '' && !(type in ['string', 'integer', 'float', 'boolean'])) {
                errors.error(
                    "Invalid type '${type}' for samplesheet field '${key}'. " +
                    'Expected one of: string, integer, float, boolean'
                )
            }
        }

        Object convert(OutputEntry entry) {
            Object value = entry.get(source)
            if (value != null && type != '') {
                try {
                    switch (type) {
                        case 'integer':
                            return value as Integer
                        case 'float':
                            return value as Float
                        case 'boolean':
                            return value as Boolean
                        case 'string':
                            return value as String
                    }
                /* groovylint-disable-next-line CatchException */
                } catch (Exception e) {
                    throw new WorksheetException(
                        "Failed to convert samplesheet field '${key}' to ${type}: ${e.message}"
                    )
                }
            }
            return value
        }

    }

}
