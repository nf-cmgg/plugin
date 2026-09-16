package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic

/**
 * Thrown when a worksheet is invalid or cannot be applied.
 * This does not abort the Nextflow pipeline; callers must catch it and
 * disable samplesheet generation instead.
 */
@CompileStatic
class WorksheetException extends RuntimeException {

    WorksheetException(String message) {
        super(message)
    }

}
