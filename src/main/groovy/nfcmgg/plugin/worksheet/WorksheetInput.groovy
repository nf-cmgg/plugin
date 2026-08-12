package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * A class used to define the expected fields of the input samplesheet
 */
@CompileStatic
@Slf4j
class WorksheetInput {

    /**
     * Field definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetInput(Map input) {
        if (input == null || input.isEmpty()) {
            log.error('Worksheet input is missing or empty')
        }
        final Map<String, Field> parsed = [:]
        input.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap)
        }
        fields = parsed.asImmutable()
    }

    /**
     * A single input field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final String name
        final String type
        final Object defaultValue

        Field(String key, Map fieldMap) {
            this.key = key
            defaultValue = fieldMap.containsKey('default') ? fieldMap.default : null
            name = fieldMap.name != null ? fieldMap.name as String : key
            final String fieldType = fieldMap.type != null ? fieldMap.type as String : 'string'
            if (!(fieldType in ['string', 'integer', 'float', 'boolean'])) {
                log.error(
                "Invalid type '${fieldType}' for input field '${key}'. Expected one of: string, integer, float, boolean"
                )
            }
            type = fieldType
        }

    }

}
