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
import java.util.regex.Matcher

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

        // Get metrics after demultiplexing
        if (targetPath.endsWith('_SAV_data') && event.source.isDirectory()) {
            Path bysamples = event.source.resolve('multiqc_bclconvert_bysample.txt')
            if (bysamples.exists()) {
                List<Map> sampleMetrics = bysamples.splitCsv(header:true, sep:'\t')
                sampleMetrics.each { metric ->
                    String sample = metric['Sample']
                    String yield = metric['yield_']
                    entries.putIfAbsent(
                        sample,
                        /* groovylint-disable-next-line UnnecessaryCast */
                        new OutputEntry(['id': sample] as Map<String, Object> + getDefaultValuesForSample(sample))
                    )
                    entries[sample].add('yield', yield)
                }
            }
        }

        // Get specific files
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
            case ~/^.*R1_\d\d\d\.fastq\.gz$/:
                entries[safeGetSample(targetName)].append('fastq_1', targetPath)
                break
            case ~/^.*R2_\d\d\d\.fastq\.gz$/:
                entries[safeGetSample(targetName)].append('fastq_2', targetPath)
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
        entries = entries.sort()
        Map<String, OutputEntry> humanEntries = entries.findAll { entry ->
            // Only retain samples of human data for the samplesheets
            entry.value.getAsString('organism')?.toLowerCase() == 'homo sapiens' ||
            entry.value.getAsString('genome')?.toLowerCase() == 'grch38'
        }

        Map<String, OutputEntry> mouseEntries = entries.findAll { entry ->
            // Only retain samples of mouse data for the samplesheets
            entry.value.getAsString('organism')?.toLowerCase() == 'mus musculus' ||
            entry.value.getAsString('genome')?.toLowerCase() == 'mm10'
        }

        //
        // nf-cmgg/sampletracking samplesheet
        //
        creator.dump(
            humanEntries
                .findAll { entry ->
                    // Only create samplesheet for WES and WGS runs of DNA samples
                    entry.value.getAsString('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.getAsString('tag')?.toLowerCase() in ['wes', 'wgs']
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

        Map<String, OutputEntry> rnafusionEntries = humanEntries
            .findAll { entry ->
                // Only create samplesheet for RNAseqMDG runs of RNA samples that have FASTQ output
                entry.value.getAsString('sample_type')?.toLowerCase() == 'rna' &&
                entry.value.getAsString('tag')?.toLowerCase() == 'rnaseqmdg' &&
                entry.value.getAsString('fastq_1')
            }
        List rnafusionKeys = [
            ['id', 'sample'],
            'fastq_1',
            'fastq_2',
            'strandedness',
            ['yield', 'reads']
        ]

        // Passed data
        List<OutputEntry> passedEntries = []
        rnafusionEntries
            .findAll { entry ->
                entry.value.getAsString('yield').toLong() >= 1000000L
            }
            .values()
            *.subKeys(rnafusionKeys)
            .each { OutputEntry entry ->
                Map<String, String> fastq2Lanes = (entry.get('fastq_2', []) as List<String>).collectEntries { fastq2 ->
                    Matcher match = fastq2 =~ ~/^.*R2_(\d\d\d)\.fastq\.gz$/
                    if (!match.find()) {
                        log.warn("Could not find lane for fastq2 file '$fastq2', skipping this file")
                        return
                    }
                    String lane = match.group(1)
                    [lane, fastq2]
                }

                (entry.get('fastq_1') as List<String>).sort().each { fastq1 ->
                    Matcher match = fastq1 =~ ~/^.*R1_(\d\d\d)\.fastq\.gz$/
                    if (!match.find()) {
                        log.warn("Could not find lane for fastq1 file '$fastq1', skipping this file")
                        return
                    }
                    String lane = match.group(1)
                    String fastq2 = fastq2Lanes.get(lane, '')
                    passedEntries << new OutputEntry(entry.values.clone() as Map<String,Object>)
                        .add('fastq_1', fastq1)
                        .add('fastq_2', fastq2)
                }
            }

        creator.dump(
            passedEntries,
            location.resolve('nfcore_rnafusion_samplesheet.yaml')
        )

        // Failed data
        List<OutputEntry> failedEntries = []
        rnafusionEntries
            .findAll { entry ->
                entry.value.getAsString('yield').toLong() < 1000000L
            }
            .values()
            *.subKeys(rnafusionKeys)
            .each { OutputEntry entry ->
                Map<String, String> fastq2Lanes = (entry.get('fastq_2', []) as List<String>).collectEntries { fastq2 ->
                    Matcher match = fastq2 =~ ~/^.*R2_(\d\d\d)\.fastq\.gz$/
                    if (!match.find()) {
                        log.warn("Could not find lane for fastq2 file '$fastq2', skipping this file")
                        return
                    }
                    String lane = match.group(1)
                    [lane, fastq2]
                }

                (entry.get('fastq_1') as List<String>).sort().each { fastq1 ->
                    Matcher match = fastq1 =~ ~/^.*R1_(\d\d\d)\.fastq\.gz$/
                    if (!match.find()) {
                        log.warn("Could not find lane for fastq1 file '$fastq1', skipping this file")
                        return
                    }
                    String lane = match.group(1)
                    String fastq2 = fastq2Lanes.get(lane, '')
                    failedEntries << new OutputEntry(entry.values.clone() as Map<String,Object>)
                        .add('fastq_1', fastq1)
                        .add('fastq_2', fastq2)
                }
            }

        creator.dump(
            failedEntries,
            location.resolve('nfcore_rnafusion_samplesheet_failed.yaml')
        )

        //
        // nf-cmgg/vivar samplesheet
        //
        creator.dump(
            (humanEntries + mouseEntries)
                .findAll { entry ->
                    String type = entry.value.getAsString('sample_type')?.toLowerCase()
                    // Only create samplesheet for DNA and tissue samples that are not mitochondrial
                    (type == 'dna' || type == 'tissue') && !entry.value.getAsString('id')?.startsWith('mtD')
                }
                .values()
                *.subKeys([
                    'id',
                    'organism',
                    'tag',
                    'binsize',
                    ['vivar_project', 'project'],
                    'normdup',
                    'nipt',
                    ['family_number', 'proband'],
                    ['cram', 'reads'],
                    ['crai', 'reads_index']
                ]),
            location.resolve('nfcmgg_vivar_samplesheet.yaml')
        )

        //
        // nf-cmgg/exomecnv samplesheet
        //
        creator.dump(
            humanEntries
                .findAll { entry ->
                    // Only create samplesheet for WES runs of DNA samples
                    entry.value.getAsString('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.getAsString('tag')?.toLowerCase() in ['wes']
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
            humanEntries
                .findAll { entry ->
                    // Only create samplesheet for DNA samples
                    entry.value.getAsString('sample_type')?.toLowerCase() == 'dna' &&
                    entry.value.getAsString('tag')?.toLowerCase() in ['wes', 'wgs']
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
        String genome = sampleData.get('genome', null)
        String organism = sampleData.get('organism', null)
        Integer binsize = sampleData.get('binsize', null) as Integer
        if (
            (organism?.toLowerCase() == 'mus musculus' || genome?.toLowerCase() == 'mm10') &&
            ![100, 500].contains(binsize)
        ) {
            binsize = 100
        }
        return [
            'strandedness': 'unknown',
            'tag': tag,
            'organism': organism,
            'genome': genome,
            'sample_type': sampleType,
            'normdup': tag.toLowerCase() == 'copgt-m',
            'nipt': tag.toLowerCase() == 'cfdnaseq',
            'binsize': binsize,
            'vivar_project': sampleData.get('vivar_project', null),
            'family': sampleData.get('family_number', null),
            'library': sampleData.get('library', null),
            'sex': sampleData.get('sex', 'U'),
            'exomecnv_batch': (sampleData.get('library', null) as String) + '_' + sampleData.get('sex', 'U'),
            'yield': -1
        ]
    }

}
