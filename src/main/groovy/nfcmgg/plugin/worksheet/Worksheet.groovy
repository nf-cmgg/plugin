package nfcmgg.plugin.worksheet

import groovy.transform.CompileStatic

import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

/**
 * A class used to define the structure of the worksheet
 */
@CompileStatic
class Worksheet {

    final String name
    final String idField
    final WorksheetInput input
    final WorksheetValues values
    final WorksheetOutput output
    final WorksheetMetrics metrics
    final WorksheetSamplesheets samplesheets

    final Set<String> dataFields = [] as Set<String>

    Worksheet(Path worksheet) {
        final WorksheetErrors errors = new WorksheetErrors()
        final Map worksheetMap = loadWorksheetMap(worksheet)

        if (worksheetMap.name == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'name' field")
        }
        name = worksheetMap.name as String

        if (worksheetMap.input == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'input' field")
        } else {
            try {
                input = new WorksheetInput(worksheetMap.input as Map)
                dataFields.addAll(input.fields.keySet())
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }

        if (worksheetMap.id_field == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'id_field' field")
        } else if (input != null && !input.hasSource(worksheetMap.id_field.toString())) {
            errors.error(
                "Worksheet ${worksheet.name} 'id_field' '${worksheetMap.id_field}' is not defined" +
                ' as a source or key (when source is missing) in the input block'
            )
        }
        idField = worksheetMap.id_field as String

        if (worksheetMap.output != null) {
            try {
                output = new WorksheetOutput(worksheetMap.output as Map)
                dataFields.addAll(output.fields.keySet())
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }

        if (worksheetMap.metrics != null) {
            try {
                metrics = new WorksheetMetrics(worksheetMap.metrics as Map)
                dataFields.addAll(metrics.fields.keySet())
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }

        if (worksheetMap.values != null) {
            try {
                values = new WorksheetValues(worksheetMap.values as Map, dataFields)
                dataFields.addAll(values.fields.keySet())
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }

        if (worksheetMap.samplesheets == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'samplesheets' field")
        } else {
            try {
                final WorksheetSamplesheetsSettings samplesheetSettings = new WorksheetSamplesheetsSettings(
                    worksheetMap.samplesheets_settings as Map, dataFields
                )
                samplesheets = new WorksheetSamplesheets(
                    worksheetMap.samplesheets as List<Map>, dataFields, samplesheetSettings
                )
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }

        errors.throwIfAny("Worksheet ${worksheet.name} is invalid")
    }

    private static Map loadWorksheetMap(Path worksheet) {
        try {
            final Map worksheetMap = new Yaml().load(worksheet.text) as Map
            if (worksheetMap == null) {
                throw new WorksheetException("Worksheet ${worksheet.name} is empty")
            }
            return worksheetMap
        } catch (WorksheetException e) {
            throw e
        /* groovylint-disable-next-line CatchException */
        } catch (Exception e) {
            throw new WorksheetException("Failed to parse worksheet ${worksheet.name}: ${e.message}")
        }
    }

}
