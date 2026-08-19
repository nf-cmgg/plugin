package nfcmgg.plugin.worksheet

import static nfcmgg.plugin.utils.FilesHelper.readFile

import nextflow.Nextflow

import java.nio.file.Path
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import groovy.transform.CompileStatic

/**
 * A class used to define how to fetch metrics for specific samples from MULTIQC files
 */
@CompileStatic
class WorksheetMetrics {

    /**
     * Metric definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetMetrics(Map metrics) {
        final WorksheetErrors errors = new WorksheetErrors()
        if (metrics == null || metrics.isEmpty()) {
            errors.error('Worksheet metrics is missing or empty')
            errors.throwIfAny('Invalid worksheet metrics')
        }
        final Map<String, Field> parsed = [:]
        metrics.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap, errors)
        }
        fields = parsed.asImmutable()
        errors.throwIfAny('Invalid worksheet metrics')
    }

    List<Field> matchingFields(String targetName) {
        return fields.values().findAll { field -> targetName.matches(field.pattern) }.toList()
    }

    /**
     * A single metric field definition
     */
    @CompileStatic
    static class Field {

        final String key
        final String pattern
        final String subpath
        final String filetype
        final String id
        final String field

        Field(String key, Map fieldMap, WorksheetErrors errors) {
            this.key = key
            if (fieldMap.pattern == null) {
                errors.error("Metric field '${key}' is missing the required 'pattern' field")
            }
            pattern = fieldMap.pattern as String
            if (pattern != null) {
                try {
                    Pattern.compile(pattern)
                } catch (PatternSyntaxException e) {
                    errors.error("Invalid regex pattern '${pattern}' for metric field '${key}': ${e.message}")
                }
            }
            subpath = fieldMap.subpath != null ? fieldMap.subpath as String : null
            final String fieldType = fieldMap.filetype != null ? fieldMap.filetype as String : null
            final List<String> allowedTypes = ['tsv', 'csv', 'yml', 'yaml', 'json']
            if (fieldType != null && !(fieldType in allowedTypes)) {
                errors.error(
                    "Invalid filetype '${fieldType}' for metric field '${key}'." +
                    " Expected one of: ${allowedTypes.join(', ')}"
                )
            }
            filetype = fieldType
            if (fieldMap.id == null) {
                errors.error("Metric field '${key}' is missing the required 'id' field")
            }
            id = fieldMap.id as String
            if (fieldMap.field == null) {
                errors.error("Metric field '${key}' is missing the required 'field' field")
            }
            this.field = fieldMap.field as String
        }

        Map<String, Object> convert(String fullPath) {
            try {
                Path targetPath = Nextflow.file(fullPath) as Path
                if (subpath) {
                    targetPath = targetPath.resolve(subpath)
                }
                List<Map<String, Object>> data = readFile(targetPath, filetype)
                Map<String, Object> result = [:]
                data.each { Map<String, Object> row ->
                    result[row[id].toString()] = row[this.field]
                }
                return result
            } catch (WorksheetException e) {
                throw e
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                throw new WorksheetException(
                    "Failed to read metrics field '${key}' from '${fullPath}': ${e.message}"
                )
            }
        }

    }

}
