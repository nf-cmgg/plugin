package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Collects worksheet validation errors so every problem is logged, then
 * throws a single {@link WorksheetException} without aborting the pipeline.
 */
@CompileStatic
@Slf4j
class WorksheetErrors {

    private final List<String> messages = []

    void error(String message) {
        log.error(message)
        messages.add(message)
    }

    /**
     * Record a message that was already logged (for example a child-block exception).
     */
    void record(String message) {
        messages.add(message)
    }

    boolean hasErrors() {
        return !messages.isEmpty()
    }

    void throwIfAny(String summary) {
        if (messages.isEmpty()) {
            return
        }
        throw new WorksheetException("${summary}:\n- ${messages.join('\n- ')}")
    }

}
