package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic

/**
 * A class used to define the expected fields of the input samplesheet
 */
@CompileStatic
class WorksheetInput {

    /**
     * Field definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetInput(Map input) {
        final WorksheetErrors errors = new WorksheetErrors()
        if (input == null || input.isEmpty()) {
            errors.error('Worksheet input is missing or empty')
            errors.throwIfAny('Invalid worksheet input')
        }
        final Map<String, Field> parsed = [:]
        input.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap, errors)
        }
        fields = parsed.asImmutable()
        errors.throwIfAny('Invalid worksheet input')
    }

    /**
     * Convert the input data to the expected format
     * @param inputData the input data to convert
     * @return the converted data
     */
    Map<String, Object> convert(Map<String, Object> inputData) {
        Map<String, Object> convertedData = [:]
        fields.each { key, field ->
            convertedData[key] = field.convert(inputData.get(field.source, null))
        }
        return convertedData
    }

    Boolean hasSource(String source) {
        return fields.any { key, field -> field.source == source }
    }

    /**
     * A single input field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final String source
        final String type
        final Object defaultValue

        Field(String key, Map fieldMap, WorksheetErrors errors) {
            this.key = key
            defaultValue = fieldMap.containsKey('default') ? fieldMap.default : null
            source = fieldMap.source != null ? fieldMap.source as String : key
            final String fieldType = fieldMap.type != null ? fieldMap.type as String : 'string'
            if (!(fieldType in ['string', 'integer', 'float', 'boolean'])) {
                errors.error(
                    "Invalid type '${fieldType}' for input field '${key}'. " +
                    'Expected one of: string, integer, float, boolean'
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
            try {
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
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                throw new WorksheetException(
                    "Failed to convert input field '${key}' to ${type}: ${e.message}"
                )
            }
            return value.toString()
        }

    }

}
