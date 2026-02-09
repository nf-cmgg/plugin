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
import nextflow.trace.event.FilePublishEvent
import nextflow.trace.TraceObserverV2

/**
 * Create samplesheets for pipelines after nf-cmgg/preprocessing
 */
@Slf4j
@CompileStatic
class PreprocessingObserver implements TraceObserverV2 {

    final private SamplesheetCreator creator = new SamplesheetCreator()

    private Map<String, OutputEntry> entries = new ConcurrentHashMap<>()
    private Path location
    private Session session

    PreprocessingObserver(Path location) {
        this.location = location
    }

    @Override
    void onFlowCreate(Session session) {
        this.location = this.location ?: getSamplesheetOutdir(session)
        this.session = session
        log.info("Samplesheets will be generated in '$location'")
    }

    @Override
    void onFilePublish(FilePublishEvent event) {
        String targetName = event.target.name
        String targetPath = event.target.toUriString()
        switch (targetName) {
            case ~/^.*\.cram$/:
                entries[safeGetSample(targetName)].add('cram', targetPath)
                break
            case ~/^.*\.crai$/:
                entries[safeGetSample(targetName)].add('crai', targetPath)
                break
            case ~/^.*R1_00.\.fastq\.gz$/:
                entries[safeGetSample(targetName)].add('fastq_1', targetPath)
                break
            case ~/^.*R2_00.\.fastq\.gz$/:
                entries[safeGetSample(targetName)].add('fastq_2', targetPath)
                break
        }
    }

    @Override
    void onFlowComplete() {
        if (session.success) {
            entries = entries.sort()
            // nf-cmgg/sampletracking samplesheet
            creator.dump(
                entries.values()*.subKeys(['id', 'cram', 'crai']),
                location.resolve('nfcmgg_sampletracking_samplesheet.yaml')
            )
            // nf-core/rnafusion samplesheet
            creator.dump(
                entries.values()*.subKeys(['id', 'fastq_1', 'fastq_2', 'strandedness']),
                location.resolve('nfcore_rnafusion_samplesheet.yaml')
            )
        }
    }

    private String safeGetSample(String basePath) {
        String sample = sampleFromPath(basePath)
        entries.putIfAbsent(sample, new OutputEntry(['id': sample, 'strandedness': 'unknown']))
        return sample
    }

}
