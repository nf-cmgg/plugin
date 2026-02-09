/*
 * Copyright 2026, Center for Medical Genetics Ghent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nfcmgg.plugin

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactoryV2

import nfcmgg.plugin.samplesheets.PreprocessingObserver
import nfcmgg.plugin.config.CmggConfig

/**
 * Implements a factory object required to create
 * the {@link CmggObserver} instance.
 */
@Slf4j
@CompileStatic
class CmggFactory implements TraceObserverFactoryV2 {

    @Override
    Collection<TraceObserverV2> create(Session session) {
        CmggConfig config = new CmggConfig(session.config?.navigate('cmgg') as Map ?: [:])
        Collection<TraceObserverV2> observers = []

        if (config.done.enabled) {
            observers << new DoneObserver(config.done.location)
        }

        if (config.samplesheets.enabled) {
            // TODO implement proper auth fetching via config
            // new SmapleAuth(
            //     System.getenv('SMAPLE_URL'),
            //     System.getenv('SMAPLE_USERNAME'),
            //     System.getenv('SMAPLE_PASSWORD')
            // ).login()
            String pipelineName = session?.manifest?.name
            if (!pipelineName) {
                log.info(
                    'Cannot determine pipeline name from session manifest, skipping automatic samplesheet generation'
                )
                return observers
            }

            log.info("Detected pipeline name: '${pipelineName}', checking for automatic samplesheet generation")

            switch (pipelineName) {
                case 'nf-cmgg/preprocessing':
                    observers << new PreprocessingObserver(config.samplesheets.location)
                    break
                default:
                    log.info('No automatic samplesheet generation possible for the current pipeline')
            }
        }
        return observers
    }

}
