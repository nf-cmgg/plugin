package nfcmgg.plugin.worksheet

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.codehaus.groovy.control.CompilationFailedException

/**
 * A class used to define extra values to be added to the input data
 */
@CompileStatic
@Slf4j
class WorksheetValues {

    private static final Pattern INPUT_FIELD_PATTERN = Pattern.compile(/input\.([A-Za-z_][A-Za-z0-9_]*)/)

    /**
     * Value definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetValues(Map values, Set<String> inputFields) {
        if (values == null || values.isEmpty()) {
            log.error('Worksheet values is missing or empty')
        }
        final Map<String, Field> parsed = [:]
        values.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap, inputFields)
        }
        fields = parsed.asImmutable()
    }

    /**
     * A single value field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final Object value
        final String func

        Field(String key, Map fieldMap, Set<String> inputFields) {
            this.key = key
            value = fieldMap.containsKey('value') ? fieldMap.value : null
            func = fieldMap.func != null ? fieldMap.func as String : null
            if (func != null) {
                validateFunc(key, func, inputFields)
            }
        }

        private static void validateFunc(String key, String func, Set<String> inputFields) {
            try {
                new GroovyShell().parse(func)
            } catch (CompilationFailedException e) {
                log.error("Invalid Groovy expression in values field '${key}': ${e.message}")
            }
            final Matcher matcher = INPUT_FIELD_PATTERN.matcher(func)
            while (matcher.find()) {
                final String referenced = matcher.group(1)
                if (!inputFields.contains(referenced)) {
                    log.error(
                        "Values field '${key}' references unknown input field 'input.${referenced}'. " +
                        "Available input fields: ${inputFields.sort().join(', ')}"
                    )
                }
            }
        }

    }

}
