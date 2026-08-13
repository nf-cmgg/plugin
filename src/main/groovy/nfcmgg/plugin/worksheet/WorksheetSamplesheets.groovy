package nfcmgg.plugin.worksheet

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.codehaus.groovy.control.CompilationFailedException

/**
 * A class used to define which samplesheets should be generated and what their content is
 */
@CompileStatic
@Slf4j
class WorksheetSamplesheets {

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

    /**
     * A single samplesheet definition
     */
    @CompileStatic
    static class Samplesheet {

        final String name
        final String includeFunc
        final String filterFunc
        final Map<String, Field> fields

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
            final Map<String, Field> parsed = [:]
            fieldsMap.each { key, value ->
                final String fieldName = key as String
                parsed[fieldName] = new Field(fieldName, value)
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
