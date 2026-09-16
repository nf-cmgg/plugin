/* groovylint-disable JUnitPublicNonTestMethod, MethodName */
package nfcmgg.plugin.worksheet

import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileDynamic

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Worksheet validation: abort on structural issues, skip bad samplesheet entries.
 */
@CompileDynamic
class WorksheetTest extends Specification {

    private static final String VALID_YAML = '''
name: test/pipeline
id_field: sample
input:
  sample:
output:
  bam:
    pattern: '.*\\.bam$'
samplesheets:
  - name: out.yaml
    fields:
      sample:
'''

    @TempDir
    private Path tempDir

    void 'valid worksheet loads'() {
        when:
        Worksheet worksheet = load(VALID_YAML)

        then:
        worksheet.name == 'test/pipeline'
        worksheet.idField == 'sample'
        worksheet.samplesheets.samplesheets.size() == 1
        worksheet.samplesheets.samplesheets[0].name == 'out.yaml'
    }

    void 'missing required top-level fields abort the plugin'() {
        when:
        load('unused: true\n')

        then:
        WorksheetException e = thrown()
        e.message.contains("missing the required 'name' field")
        e.message.contains("missing the required 'input' field")
        e.message.contains("missing the required 'id_field' field")
        e.message.contains("missing the required 'samplesheets' field")
    }

    void 'collects multiple input field errors before aborting'() {
        when:
        new WorksheetInput([
            age: [type: 'nope'],
            count: [type: 'also_nope']
        ])

        then:
        WorksheetException e = thrown()
        e.message.contains("Invalid type 'nope' for input field 'age'")
        e.message.contains("Invalid type 'also_nope' for input field 'count'")
    }

    void 'invalid values func aborts the plugin'() {
        when:
        new WorksheetValues(
            [derived: [func: 'input.missing * 2']],
            ['sample'] as Set
        )

        then:
        WorksheetException e = thrown()
        e.message.contains("unknown input field 'input.missing'")
    }

    void 'missing output pattern aborts the plugin'() {
        when:
        new WorksheetOutput([bam: [:]])

        then:
        WorksheetException e = thrown()
        e.message.contains("Output field 'bam' is missing the required 'pattern' field")
    }

    void 'invalid metric definition aborts the plugin'() {
        when:
        new WorksheetMetrics([yield: [pattern: '.*', filetype: 'xlsx']])

        then:
        WorksheetException e = thrown()
        e.message.contains("Invalid filetype 'xlsx' for metric field 'yield'")
        e.message.contains("missing the required 'id' field")
        e.message.contains("missing the required 'field' field")
    }

    void 'invalid samplesheet entries are skipped and valid ones are kept'() {
        when:
        WorksheetSamplesheets sheets = new WorksheetSamplesheets([
            [fields: [sample: [:]]],
            [
                name: 'good.yaml',
                fields: [sample: [:]]
            ],
            [
                name: 'bad-func.yaml',
                include_func: 'data.unknown == true',
                fields: [sample: [:]]
            ]
        ], ['sample'] as Set)

        then:
        sheets.samplesheets.size() == 1
        sheets.samplesheets[0].name == 'good.yaml'
    }

    void 'empty samplesheets block aborts because the block itself is required'() {
        when:
        new WorksheetSamplesheets([], ['sample'] as Set)

        then:
        thrown(WorksheetException)
    }

    void 'worksheet with one bad samplesheet still loads'() {
        when:
        Worksheet worksheet = load('''
name: test/pipeline
id_field: sample
input:
  sample:
output:
  bam:
    pattern: '.*\\.bam$'
samplesheets:
  - name: bad.yaml
    include_func: data.does_not_exist == true
    fields:
      sample:
  - name: good.yaml
    fields:
      sample:
''')

        then:
        worksheet.samplesheets.samplesheets.size() == 1
        worksheet.samplesheets.samplesheets[0].name == 'good.yaml'
    }

    void 'id_field that is not an input source aborts the plugin'() {
        when:
        load('''
name: test/pipeline
id_field: samplename
input:
  sample:
output:
  bam:
    pattern: '.*\\.bam$'
samplesheets:
  - name: out.yaml
    fields:
      sample:
''')

        then:
        WorksheetException e = thrown()
        e.message.contains("'id_field' 'samplename' is not defined")
    }

    private Worksheet load(String yaml) {
        Path file = tempDir.resolve('worksheet.yml')
        Files.writeString(file, yaml.stripIndent())
        return new Worksheet(file)
    }

}
