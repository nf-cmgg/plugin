package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * A class used to define which output files should be remembered for samplesheet generation
 */
@CompileStatic
@Slf4j
class WorksheetOutput {

    /**
     * Output definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetOutput(Map output) {
        if (output == null || output.isEmpty()) {
            log.error('Worksheet output is missing or empty')
        }
        final Map<String, Field> parsed = [:]
        output.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap)
        }
        fields = parsed.asImmutable()
    }

    /**
     * A single output field definition
     */
    @CompileStatic
    @Slf4j
    static class Field {

        final String key
        final String pattern

        Field(String key, Map fieldMap) {
            this.key = key
            if (fieldMap.pattern == null) {
                log.error("Output field '${key}' is missing the required 'pattern' field")
            }
            pattern = fieldMap.pattern as String
        }

    }

}
