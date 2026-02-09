/* groovylint-disable JUnitPublicNonTestMethod, MethodName */
package nfcmgg.plugin.samplesheets

import java.nio.file.Path
import java.nio.file.Files

import groovy.transform.CompileDynamic

import spock.lang.Specification

/**
 * Implements a basic factory test
 */
@CompileDynamic
class SamplesheetCreatorTest extends Specification {

    void 'should create an instance'() {
        given:
        SamplesheetCreator creator = new SamplesheetCreator()
        expect:
        creator in SamplesheetCreator
    }

    void 'should create a samplesheet'() {
        when:
        SamplesheetCreator creator = new SamplesheetCreator()
        Path tmpFile = createTempFile()
        List<OutputEntry> entries = [
            new OutputEntry([id: 'test', file: 'test.cram', sample: 'sample1']),
            new OutputEntry([id: 'test2', file: 'test2.cram', sample: 'sample2'])
        ]
        then:
        creator.dump(entries, tmpFile)
        expect:
        tmpFile.text == '''- id: test
  file: test.cram
  sample: sample1
- id: test2
  file: test2.cram
  sample: sample2
'''
    }

    /* groovylint-disable-next-line FactoryMethodName */
    private Path createTempFile() {
        Path tmpFile = Files.createTempFile('test', '.yaml')
        tmpFile.toFile().deleteOnExit()
        return tmpFile
    }

}
