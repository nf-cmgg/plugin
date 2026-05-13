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
            case ~/^snp_.*\.cram$/:
                entries[safeGetSample(targetName)].add('snp_cram', targetPath)
                break
            case ~/^snp_.*\.cram\.crai$/:
                entries[safeGetSample(targetName)].add('snp_crai', targetPath)
                break
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
            case ~/^.*\.per-base\.bed\.gz$/:
                entries[safeGetSample(targetName)].add('per_base_bed', targetPath)
                break
            case ~/^.*\.per-base\.bed\.gz\.csi$/:
                entries[safeGetSample(targetName)].add('per_base_bed_index', targetPath)
                break
        }
    }

    /* groovylint-disable-next-line MethodSize */
    @Override
    void onFlowComplete() {
        if (!session.success) { return }
        entries = entries.findAll { entry ->
            // Only retain diagnostic samples of human data for the samplesheets
            entry.value.get('purpose')?.toLowerCase() == 'diagnostic' &&
            (
                entry.value.get('organism')?.toLowerCase() == 'homo sapiens' ||
                entry.value.get('genome')?.toLowerCase() == 'grch38'
            )
        }
        entries = entries.sort()

        //
        // nf-cmgg/sampletracking samplesheet
        //
        creator.dump(
            entries
                .findAll { entry ->
                    // Only create samplesheet for WES and WGS runs of DNA samples
                    entry.value.get('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.get('tag')?.toLowerCase() in ['wes', 'wgs']
                }
                .values()
                *.subKeys([
                    ['id', 'sample'],
                    ['library', 'pool'],
                    ['cram', 'sample_bam'],
                    ['crai', 'sample_bam_index'],
                    ['snp_cram', 'snp_bam'],
                    ['snp_crai', 'snp_bam_index'],
                    'sex'
                ]),
            location.resolve('nfcmgg_sampletracking_samplesheet.yaml')
        )

        //
        // nf-core/rnafusion samplesheet
        //
        creator.dump(
            entries
                .findAll { entry ->
                    // Only create samplesheet for RNAseqMDG runs of RNA samples that have FASTQ output
                    entry.value.get('sample_type')?.toLowerCase() == 'rna' &&
                    entry.value.get('tag')?.toLowerCase() == 'rnaseqmdg' &&
                    entry.value.get('fastq_1')
                }
                .values()
                *.subKeys([
                    ['id', 'sample'],
                    'fastq_1',
                    'fastq_2',
                    'strandedness'
                ]),
            location.resolve('nfcore_rnafusion_samplesheet.yaml')
        )

        //
        // nf-cmgg/vivar samplesheet
        //
        creator.dump(
            entries
                .findAll { entry ->
                    // Only create samplesheet for DNA samples
                    entry.value.get('sample_type')?.toLowerCase() == 'dna'
                }
                .values()
                *.subKeys([
                    'id',
                    'organism',
                    'tag',
                    'binsize',
                    ['vivar_project', 'project'],
                    'sex',
                    'normdup',
                    'nipt',
                    ['cram', 'reads'],
                    ['crai', 'reads_index']
                ]),
            location.resolve('nfcmgg_vivar_samplesheet.yaml')
        )

        //
        // nf-cmgg/exomecnv samplesheet
        //
        creator.dump(
            entries
                .findAll { entry ->
                    // Only create samplesheet for WES runs of DNA samples
                    entry.value.get('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.get('tag')?.toLowerCase() in ['wes']
                }
                .values()
                *.subKeys([
                    ['id', 'sample'],
                    ['exomecnv_batch', 'batch'],
                    'family',
                    'cram',
                    'crai',
                    ['per_base_bed', 'bed'],
                    ['per_base_bed_index', 'bed_index']
                ]),
            location.resolve('nfcmgg_exomecnv_samplesheet.yaml')
        )

        //
        // nf-cmgg/smallvariants samplesheet
        //
        creator.dump(
            entries
                .findAll { entry ->
                    // Only create samplesheet for DNA samples
                    entry.value.get('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.get('tag')?.toLowerCase() in ['wes', 'wgs']
                }
                .values()
                *.subKeys([
                    ['id', 'sample'],
                    'cram',
                    'crai'
                ]),
            location.resolve('nfcmgg_smallvariants_samplesheet.yaml')
        )
    }

    @Override
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
            'vivar_project': sampleData.get('vivar_project', null),
            'family': sampleData.get('family_number', null),
            'library': sampleData.get('library', null),
            'sex': sampleData.get('sex', 'U'),
            'exomecnv_batch': (sampleData.get('library', null) as String) + '_' + sampleData.get('sex', 'U'),
            'purpose': sampleData.get('purpose', null),
        ]
    }

}
