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

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import groovy.transform.Canonical

import nextflow.Session
import nextflow.trace.event.FilePublishEvent
import nextflow.trace.TraceObserverV2

/**
 * Create samplesheets for pipelines after nf-cmgg/preprocessing
 */
@Slf4j
@CompileStatic
class PreprocessingObserver implements TraceObserverV2 {

    Map<String, NfCmggPreprocessingOutputEntry> entries = Collections.synchronizedMap([:])

    @Override
    void onFlowCreate(Session session) {
        log.info('Automatic samplesheet generation detected')
    }

    @Override
    void onFilePublish(FilePublishEvent event) {
        String targetName = event.target.name
        String targetPath = event.target.toUri()
        switch (targetName) {
            case ~/^.*\.cram$/:
                entries[safeGetSample(targetName)].cram = targetPath
                break
            case ~/^.*\.crai$/:
                entries[safeGetSample(targetName)].crai = targetPath
                break
            case ~/^.*R1_00.\.fastq\.gz$/:
                entries[safeGetSample(targetName)].fastq1 = targetPath
                break
            case ~/^.*R2_00.\.fastq\.gz$/:
                entries[safeGetSample(targetName)].fastq2 = targetPath
                break
        }
    }

    @Override
    void onFlowComplete() {
        println entries
    }

    private String safeGetSample(String basePath) {
        String sample = sampleFromPath(basePath)
        if (!entries.containsKey(sample)) {
            entries[sample] = new NfCmggPreprocessingOutputEntry()
        }
        return sample
    }

}

@Canonical
@CompileStatic
class NfCmggPreprocessingOutputEntry {

    String cram
    String crai
    String fastq1
    String fastq2

}
