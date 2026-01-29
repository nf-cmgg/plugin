package nfcmgg.plugin

import nextflow.Session
import spock.lang.Specification

/**
 * Implements a basic factory test
 *
 */
class CmggObserverTest extends Specification {

    def 'should create the observer instance' () {
        given:
        def factory = new CmggFactory()
        when:
        def result = factory.create(Mock(Session))
        then:
        result.size() == 1
        result.first() instanceof CmggObserver
    }

}
