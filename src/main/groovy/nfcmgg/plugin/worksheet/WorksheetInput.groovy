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
     * Convert the input data to the expected format
     * @param inputData the input data to convert
     * @return the converted data
     */
    Map<String, Object> convert(Map<String, Object> inputData) {
        Map<String, Object> convertedData = [:]
        fields.each { key, field ->
            convertedData[key] = field.convert(inputData.get(field.name, null))
        }
        return convertedData
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

        /**
         * Convert the input value to the expected format
         * @param value the input value to convert
         * @return the converted value
         */
        Object convert(Object value) {
            if (value == null) {
                return defaultValue
            }
            switch (type) {
                case 'string':
                    return value.toString()
                case 'integer':
                    return value.toString().toInteger()
                case 'float':
                    return value.toString().toFloat()
                case 'boolean':
                    return value.toString().toBoolean()
            }
            return value.toString()
        }

    }

}
