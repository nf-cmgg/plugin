/* groovylint-disable JUnitPublicNonTestMethod, MethodName */
package nfcmgg.plugin

import groovy.transform.CompileDynamic

import nextflow.Session
import spock.lang.Specification

/**
 * Implements a basic factory test
 */
@CompileDynamic
class CmggObserverTest extends Specification {

    void 'should create the observer instance'() {
        given:
        CmggFactory factory = new CmggFactory()
        expect:
        factory.create(Mock(Session)).size() == 0
    }

}
