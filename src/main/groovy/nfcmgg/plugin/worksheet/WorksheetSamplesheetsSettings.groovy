package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * A class used to define which samplesheets should be generated and what their content is
 */
@CompileStatic
@Slf4j
class WorksheetSamplesheetsSettings {

    /**
     * Samplesheet definitions in declaration order
     */
    final String splitBy = null

    WorksheetSamplesheetsSettings(Map<String,Object> samplesheetsSettings, Set<String> dataFields) {
        final WorksheetErrors errors = new WorksheetErrors()

        if (samplesheetsSettings?.containsKey('split_by')) {
            final String splitByField = samplesheetsSettings['split_by'] as String
            if (dataFields.contains(splitByField)) {
                this.splitBy = splitByField
            } else {
                errors.error("${splitByField} is not a valid `split_by` field, use one of: ${dataFields.join(', ')}")
            }
        }
    }

}
