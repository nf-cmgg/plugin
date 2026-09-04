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

    Worksheet(Path worksheet) {
        final WorksheetErrors errors = new WorksheetErrors()
        final Map worksheetMap = loadWorksheetMap(worksheet)

        if (worksheetMap.name == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'name' field")
        }
        name = worksheetMap.name as String

        WorksheetInput parsedInput = null
        if (worksheetMap.input == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'input' field")
        } else {
            try {
                parsedInput = new WorksheetInput(worksheetMap.input as Map)
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }
        input = parsedInput

        if (worksheetMap.id_field == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'id_field' field")
        } else if (parsedInput != null && !parsedInput.hasSource(worksheetMap.id_field.toString())) {
            errors.error(
                "Worksheet ${worksheet.name} 'id_field' '${worksheetMap.id_field}' is not defined" +
                ' as a source or key (when source is missing) in the input block'
            )
        }
        idField = worksheetMap.id_field as String

        final Set<String> inputFields = parsedInput != null ? parsedInput.fields.keySet() : ([] as Set<String>)
        WorksheetValues parsedValues = null
        if (worksheetMap.values != null) {
            try {
                parsedValues = new WorksheetValues(worksheetMap.values as Map, inputFields)
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }
        values = parsedValues

        WorksheetOutput parsedOutput = null
        if (worksheetMap.output == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'output' field")
        } else {
            try {
                parsedOutput = new WorksheetOutput(worksheetMap.output as Map)
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }
        output = parsedOutput

        WorksheetMetrics parsedMetrics = null
        if (worksheetMap.metrics != null) {
            try {
                parsedMetrics = new WorksheetMetrics(worksheetMap.metrics as Map)
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }
        metrics = parsedMetrics

        WorksheetSamplesheets parsedSamplesheets = null
        if (worksheetMap.samplesheets == null) {
            errors.error("Worksheet ${worksheet.name} is missing the required 'samplesheets' field")
        } else {
            final Set<String> dataFields = [] as Set
            dataFields.addAll(inputFields)
            if (parsedValues != null) {
                dataFields.addAll(parsedValues.fields.keySet())
            }
            if (parsedOutput != null) {
                dataFields.addAll(parsedOutput.fields.keySet())
            }
            if (parsedMetrics != null) {
                dataFields.addAll(parsedMetrics.fields.keySet())
            }
            try {
                parsedSamplesheets = new WorksheetSamplesheets(
                    worksheetMap.samplesheets as List<Map>, dataFields
                )
            } catch (WorksheetException e) {
                errors.record(e.message)
            }
        }
        samplesheets = parsedSamplesheets

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
