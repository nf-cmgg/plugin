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

import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.Nextflow
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactoryV2

import nfcmgg.plugin.samplesheets.PipelineObserver
import nfcmgg.plugin.config.CmggConfig
import nfcmgg.plugin.worksheet.Worksheet
import nfcmgg.plugin.worksheet.WorksheetException

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
            String pipelineName = session?.manifest?.name
            if (!pipelineName) {
                log.info(
                    'Cannot determine pipeline name from session manifest, skipping automatic samplesheet generation'
                )
                return observers
            }

            log.info("Detected pipeline name: '${pipelineName}', checking for automatic samplesheet generation")

            List<Path> worksheets = config.samplesheets.worksheets
            ((Path) Nextflow.file(getClass().getResource('/worksheets').toURI().toString())).eachFile { res ->
                worksheets << res
            }
            Worksheet worksheet
            try {
                worksheet = new Worksheet(worksheets.find { Path worksheetPath ->
                    if (['yml', 'yaml'].contains(worksheetPath.extension)) {
                        String worksheetPipelineName = ((Map)new Yaml().load(worksheetPath.text)).get('name', '')
                        if (worksheetPipelineName == pipelineName) {
                            return true
                        }
                    }
                    return false
                })
            } catch (WorksheetException e) {
                log.error("Invalid worksheet, skipping automatic samplesheet generation: ${e.message}")
                return observers
            /* groovylint-disable-next-line CatchException */
            } catch (Exception e) {
                log.error("Failed to set up samplesheet generation, continuing pipeline: ${e.message}")
                return observers
            }

            if (!worksheet) {
                log.info('No worksheet found for the current pipeline, skipping automatic samplesheet generation')
                return observers
            }

            observers << new PipelineObserver(config.samplesheets.location, worksheet)
        }
        return observers
    }

}
