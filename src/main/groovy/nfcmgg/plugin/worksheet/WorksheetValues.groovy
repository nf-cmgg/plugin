package nfcmgg.plugin.worksheet

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic

import org.codehaus.groovy.control.CompilationFailedException

import nfcmgg.plugin.utils.SafeGroovy

/**
 * A class used to define extra values to be added to the input data
 */
@CompileStatic
class WorksheetValues {

    private static final Pattern INPUT_FIELD_PATTERN = Pattern.compile(/input\.([A-Za-z_][A-Za-z0-9_]*)/)

    /**
     * Value definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetValues(Map values, Set<String> inputFields) {
        final WorksheetErrors errors = new WorksheetErrors()
        if (values == null || values.isEmpty()) {
            errors.error('Worksheet values is missing or empty')
            errors.throwIfAny('Invalid worksheet values')
        }
        final Map<String, Field> parsed = [:]
        values.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap, inputFields, errors)
        }
        fields = parsed.asImmutable()
        errors.throwIfAny('Invalid worksheet values')
    }

    Map<String, Object> convert(Map<String, Object> inputData, Map<String, Object> params) {
        Map<String, Object> convertedData = inputData
        Binding inputBinding = new Binding([input: inputData, params: params])
        GroovyShell inputShell = SafeGroovy.shell(inputBinding)
        fields.each { key, field ->
            convertedData[key] = field.define(inputShell)
        }
        return convertedData
    }

    /**
     * A single value field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final Object value
        final String func

        Field(String key, Map fieldMap, Set<String> inputFields, WorksheetErrors errors) {
            this.key = key
            value = fieldMap.containsKey('value') ? fieldMap.value : null
            func = fieldMap.func != null ? fieldMap.func as String : null
            if (value == null && func == null) {
                errors.error("Values field '${key}' must define either 'value' or 'func'")
            }
            if (value != null && func != null) {
                errors.error("Values field '${key}' cannot define both 'value' and 'func'")
            }
            if (func != null) {
                validateFunc(key, func, inputFields, errors)
            }
        }

        Object define(GroovyShell shell) {
            try {
                return value != null ? value : shell.evaluate(func)
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                throw new WorksheetException("Failed to evaluate values field '${key}': ${e.message}")
            }
        }

        private static void validateFunc(String key, String func, Set<String> inputFields, WorksheetErrors errors) {
            try {
                SafeGroovy.parse(func)
            } catch (CompilationFailedException e) {
                errors.error("Invalid Groovy expression in values field '${key}': ${e.message}")
            }
            final Matcher matcher = INPUT_FIELD_PATTERN.matcher(func)
            while (matcher.find()) {
                final String referenced = matcher.group(1)
                if (!inputFields.contains(referenced)) {
                    errors.error(
                        "Values field '${key}' references unknown input field 'input.${referenced}'. " +
                        "Available input fields: ${inputFields.sort().join(', ')}"
                    )
                }
            }
        }

    }

}
