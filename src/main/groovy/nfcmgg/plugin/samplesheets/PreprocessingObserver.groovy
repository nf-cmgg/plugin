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

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.nio.file.Path

import nextflow.trace.event.FilePublishEvent

/**
 * Create samplesheets for pipelines after nf-cmgg/preprocessing
 */
@Slf4j
@CompileStatic
class PreprocessingObserver extends PipelineObserver {

    PreprocessingObserver(Path location) {
        super(location)
        super.sampleKey = 'samplename'
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
            // nf-cmgg/vivar samplesheet
            creator.dump(
                entries.values()*.subKeys(
                    ['id', 'organism', 'tag', 'normdup', 'nipt', 'binsize', 'project_id', ['cram', 'reads'], ['crai', 'reads_index']]
                ),
                location.resolve('nfcmgg_vivar_samplesheet.yaml')
            )
        }
    }

    Map getDefaultValuesForSample(String sample) {
        Map<String,Object> sampleData = inputData.find { entry -> entry.get('samplename', '') == sample } ?: [:]
        String sampleType = sampleData.get('sample_type', 'DNA')
        String tag = sampleData.get('tag', '')
        return [
            'strandedness': 'unknown',
            'tag': tag,
            'organism': sampleData.get('organism', null),
            'genome': sampleData.get('genome', null),
            'sample_type': sampleType,
            'normdup': tag.toLowerCase() == 'copgt-m',
            'nipt': tag.toLowerCase() == 'cfdnaseq',
            'binsize': sampleData.get('binsize', null),
            'project_id': sampleData.get('project_id', null)
        ]
    }

}
