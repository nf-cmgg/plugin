/* groovylint-disable JUnitPublicNonTestMethod, MethodName */
package nfcmgg.plugin.samplesheets

import groovy.transform.CompileDynamic

import spock.lang.Specification

/**
 * Implements a basic factory test
 */
@CompileDynamic
class OutputEntryTest extends Specification {

    void 'should create an empty instance'() {
        when:
        Map<String, String> values = [:]
        then:
        OutputEntry entry = new OutputEntry(values)
        expect:
        entry in OutputEntry
        entry.values == values
    }

    void 'should create an instance with defaults'() {
        when:
        Map<String, String> values = [id: 'test']
        then:
        OutputEntry entry = new OutputEntry(values)
        expect:
        entry in OutputEntry
        entry.values == values
    }

    void 'should create an instance with added values'() {
        when:
        Map<String, String> values = [:]
        then:
        OutputEntry entry = new OutputEntry(values)
        entry.add('id', 'test')
        expect:
        entry in OutputEntry
        entry.values == values + [id: 'test']
    }

    void 'should subKey correctly'() {
        when:
        Map<String, String> values = [id:'test', file:'test.cram', sample:'sample1']
        List<String> keys = ['id', 'file']
        then:
        OutputEntry mainEntry = new OutputEntry(values)
        OutputEntry entry = mainEntry.subKeys(keys)
        expect:
        entry in OutputEntry
        entry.values == values.subMap(keys)
        mainEntry.values == values
    }

    void 'should subKey correctly with key rename'() {
        when:
        Map<String, String> values = [id:'test', file:'test.cram', sample:'sample1']
        List<Object> keys = ['id', ['file', 'cram']]
        then:
        OutputEntry mainEntry = new OutputEntry(values)
        OutputEntry entry = mainEntry.subKeys(keys)
        expect:
        entry in OutputEntry
        entry.values == [id: 'test', cram: 'test.cram']
        mainEntry.values == values
    }
}
