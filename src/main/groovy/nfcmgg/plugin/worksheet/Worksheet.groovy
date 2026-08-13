package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

/**
 * A class used to define the structure of the worksheet
 */
@CompileStatic
@Slf4j
class Worksheet {

    final String name
    final String idField
    final WorksheetInput input
    final WorksheetValues values
    final WorksheetOutput output
    final WorksheetMetrics metrics
    final WorksheetSamplesheets samplesheets

    Worksheet(Path worksheet) {
        final Map worksheetMap = new Yaml().load(worksheet.text)
        if (worksheetMap.name == null) {
            log.error("Worksheet ${worksheet.name} is missing the required 'name' field")
        }
        name = worksheetMap.name
        if (worksheetMap.id_field == null) {
            log.error("Worksheet ${worksheet.name} is missing the required 'id_field' field")
        }
        idField = worksheetMap.id_field
        if (worksheetMap.input == null) {
            log.error("Worksheet ${worksheet.name} is missing the required 'input' field")
        }
        input = new WorksheetInput(worksheetMap.input as Map)
        values = worksheetMap.values != null ? new WorksheetValues(
            worksheetMap.values as Map, input.fields.keySet()
        ) : null
        if (worksheetMap.output == null) {
            log.error("Worksheet ${worksheet.name} is missing the required 'output' field")
        }
        output = new WorksheetOutput(worksheetMap.output as Map)
        metrics = worksheetMap.metrics != null ? new WorksheetMetrics(worksheetMap.metrics as Map) : null
        if (worksheetMap.samplesheets == null) {
            log.error("Worksheet ${worksheet.name} is missing the required 'samplesheets' field")
        }
        samplesheets = new WorksheetSamplesheets(worksheetMap.samplesheets as List<Map>)
    }

}
