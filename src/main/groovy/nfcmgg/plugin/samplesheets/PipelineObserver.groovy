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
package nfcmgg.plugin.samplesheets

import static nfcmgg.plugin.utils.ParseHelper.sampleFromPath
import static nfcmgg.plugin.utils.SessionFetcher.getSamplesheetOutdir

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path

import nextflow.Session
import nextflow.trace.TraceObserverV2

/**
 * Create samplesheets for pipelines after nf-cmgg/preprocessing
 */
@Slf4j
@CompileStatic
class PipelineObserver implements TraceObserverV2 {

    final SamplesheetCreator creator = new SamplesheetCreator()

    Map<String, OutputEntry> entries = new ConcurrentHashMap<>()
    Path location
    Session session

    PipelineObserver(Path location) {
        this.location = location
    }

    @Override
    void onFlowCreate(Session session) {
        this.location = this.location ?: getSamplesheetOutdir(session)
        this.session = session
        log.info("Samplesheets will be generated in '$location'")
    }

    String safeGetSample(String basePath) {
        String sample = sampleFromPath(basePath)
        entries.putIfAbsent(sample, new OutputEntry(['id': sample, 'strandedness': 'unknown']))
        return sample
    }

}
