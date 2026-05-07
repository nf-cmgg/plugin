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
import static nfcmgg.plugin.utils.SessionFetcher.getInputSamplesheetList

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path

import nextflow.Session
import nextflow.trace.TraceObserverV2

/**
 * A base observer class to be extended per pipeline
 */
@Slf4j
@CompileStatic
class PipelineObserver implements TraceObserverV2 {

    final SamplesheetCreator creator = new SamplesheetCreator()

    Map<String, OutputEntry> entries = new ConcurrentHashMap<>()
    Path location
    List<Map<String, Object>> inputData
    Session session
    String sampleKey
    Set<String> samples

    PipelineObserver(Path location) {
        this.location = location
        this.sampleKey = 'id'
    }

    @Override
    void onFlowCreate(Session session) {
        this.location = this.location ?: getSamplesheetOutdir(session)
        this.inputData = getInputSamplesheetList(session)
        this.samples = inputData*.get(sampleKey).findAll { sample -> sample != null}.toSet() as Set<String>
        this.session = session
        log.info("Samplesheets will be generated in '$location'")
    }

    String safeGetSample(String basePath) {
        List<String> possibleSamples = samples.findAll { sample -> basePath.startsWith(sample) }.toList() as List<String>
        String sample
        switch (possibleSamples.size()) {
            case 1:
                sample = possibleSamples[0]
                break
            case 0:
                sample = sampleFromPath(basePath)
                log.warn("Could not find sample for path '$basePath' in input samplesheet, using '$sample' as sample name")
                break
            default:
                sample = possibleSamples.sort { a, b -> b.size() <=> a.size() }[0]
                log.warn("Multiple possible samples found for path '$basePath': $possibleSamples, using '$sample' as sample name, because it is the longest match")
        }
        entries.putIfAbsent(sample, new OutputEntry(['id': sample] + getDefaultValuesForSample(sample)))
        return sample
    }

    /**
     * Override this method to provide default values for samples when they are first encountered
     * @param sample the sample name for which default values should be provided
     * @return a map of key-value pairs to be added to the sample entry when it is first created
     */
    /* groovylint-disable-next-line UnusedMethodParameter */
    Map getDefaultValuesForSample(String sample) {
        return [:]
    }

}
