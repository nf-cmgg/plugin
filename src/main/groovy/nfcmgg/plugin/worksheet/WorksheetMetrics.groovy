package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * A class used to define how to fetch metrics for specific samples from MULTIQC files
 */
@CompileStatic
@Slf4j
class WorksheetMetrics {

    /**
     * Metric definitions keyed by the plugin field name
     */
    final Map<String, Field> fields

    WorksheetMetrics(Map metrics) {
        if (metrics == null || metrics.isEmpty()) {
            log.error('Worksheet metrics is missing or empty')
        }
        final Map<String, Field> parsed = [:]
        metrics.each { key, value ->
            final String fieldName = key as String
            final Map fieldMap = (value != null ? value as Map : [:]) as Map
            parsed[fieldName] = new Field(fieldName, fieldMap)
        }
        fields = parsed.asImmutable()
    }

    /**
     * A single metric field definition
     */
    @CompileStatic
    @Slf4j
    static class Field {

        final String key
        final String pattern
        final String subpath
        final String type
        final String id
        final String field

        Field(String key, Map fieldMap) {
            this.key = key
            if (fieldMap.pattern == null) {
                log.error("Metric field '${key}' is missing the required 'pattern' field")
            }
            pattern = fieldMap.pattern as String
            subpath = fieldMap.subpath != null ? fieldMap.subpath as String : null
            final String fieldType = fieldMap.type != null ? fieldMap.type as String : 'tsv'
            if (!(fieldType in ['tsv', 'csv'])) {
                log.error("Invalid type '${fieldType}' for metric field '${key}'. Expected one of: tsv, csv")
            }
            type = fieldType
            if (fieldMap.id == null) {
                log.error("Metric field '${key}' is missing the required 'id' field")
            }
            id = fieldMap.id as String
            if (fieldMap.field == null) {
                log.error("Metric field '${key}' is missing the required 'field' field")
            }
            this.field = fieldMap.field as String
        }

    }

}
