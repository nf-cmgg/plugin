package nfcmgg.plugin.worksheet

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import groovy.transform.CompileStatic

/**
 * A class used to define which output files should be remembered for samplesheet generation
 */
@CompileStatic
class WorksheetOutput {

    /**
     * Output definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetOutput(Map output) {
        final WorksheetErrors errors = new WorksheetErrors()
        if (output == null || output.isEmpty()) {
            errors.error('Worksheet output is missing or empty')
            errors.throwIfAny('Invalid worksheet output')
        }
        final Map<String, Field> parsed = [:]
        output.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap, errors)
        }
        fields = parsed.asImmutable()
        errors.throwIfAny('Invalid worksheet output')
    }

    Field matchingField(String targetName) {
        return fields.values().find { field -> targetName.matches(field.pattern) }
    }

    /**
     * A single output field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final String pattern

        Field(String key, Map fieldMap, WorksheetErrors errors) {
            this.key = key
            if (fieldMap.pattern == null) {
                errors.error("Output field '${key}' is missing the required 'pattern' field")
            }
            pattern = fieldMap.pattern as String
            if (pattern != null) {
                try {
                    Pattern.compile(pattern)
                } catch (PatternSyntaxException e) {
                    errors.error("Invalid regex pattern '${pattern}' for output field '${key}': ${e.message}")
                }
            }
        }

    }

}
