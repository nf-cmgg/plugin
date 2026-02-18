/* groovylint-disable DuplicateNumberLiteral, LineLength */
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
package nfcmgg.plugin.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Files
import java.util.zip.GZIPInputStream

/**
 * Utility functions for FastQ file processing
 */
@Slf4j
@CompileStatic
class FastqUtils {

    /**
     * Parse first line of a FASTQ file, return the flowcell id and lane number.
     *
     * @param path Path to the FastQ file
     * @return Flowcell ID or null if not found
     */
    static String flowcellLaneFromFastq(Path path) {
        // First line of FASTQ file contains sequence identifier plus optional description
        String firstLine = readFirstLineOfFastq(path)
        String flowcellId = null

        // Expected format from ILLUMINA
        // cf https://en.wikipedia.org/wiki/FASTQ_format#Illumina_sequence_identifiers
        // Five fields:
        // @<instrument>:<lane>:<tile>:<x-pos>:<y-pos>...
        // Seven fields or more (from CASAVA 1.8+):
        // "@<instrument>:<run number>:<flowcell ID>:<lane>:<tile>:<x-pos>:<y-pos>..."

        String[] fields = firstLine ? firstLine.split(':') : new String[0]
        if (fields.size() == 5) {
            // Get the instrument name as flowcell ID
            flowcellId = fields[0].substring(1)
        } else if (fields.size() >= 7) {
            // Get the actual flowcell ID
            flowcellId = fields[2]
        } else if (fields.size() != 0) {
            log.warn("FASTQ file(${path}): Cannot extract flowcell ID from ${firstLine}")
        }
        return flowcellId
    }

    /**
     * Get first line of a FASTQ file
     *
     * @param path Path to the FastQ file
     * @return The first line of the file
     */
    static String readFirstLineOfFastq(Path path) {
        String line = null
        try {
            InputStream is = Files.newInputStream(path)
            is.withCloseable { InputStream wrapped ->
                InputStream gzipStream = new GZIPInputStream(wrapped)
                Reader decoder = new InputStreamReader(gzipStream, 'ASCII')
                BufferedReader buffered = new BufferedReader(decoder)
                line = buffered.readLine()
                if (line && !line.startsWith('@')) {
                    log.warn("FASTQ file(${path}): First line does not start with '@'")
                }
            }
        } catch (IOException e) {
            log.warn("FASTQ file(${path}): Error streaming: ${e.message}")
        }
        return line
    }

    /**
     * Add readgroup to meta and remove lane
     *
     * @param meta Map containing sample metadata
     * @param files List of FastQ files
     * @param params Map containing pipeline parameters
     * @return List containing updated meta map and files
     */
    static List addReadgroupToMeta(Map meta, List<Path> files, Map params) {
        String cn = params.seq_center ? "CN:${params.seq_center}\\t" : ''
        String flowcell = flowcellLaneFromFastq(files[0])

        // Check if flowcell ID matches
        if (flowcell && files.size() > 1 && flowcell != flowcellLaneFromFastq(files[1])) {
            throw new IllegalStateException("Flowcell ID does not match for paired reads of sample ${meta.id} - ${files}")
        }

        // If we cannot read the flowcell ID from the fastq file, then we don't use it
        String sampleLaneId = flowcell ? "${flowcell}.${meta.sample}.${meta.lane}" : "${meta.sample}.${meta.lane}"

        // Don't use a random element for ID, it breaks resuming
        String readGroup = params.umi_read_structure
            ? "\"@RG\\tID:${meta.sample}\\t${cn}PU:consensus\\tSM:${meta.patient}_${meta.sample}\\tLB:${meta.sample}\\tDS:${params.fasta}\\tPL:${params.seq_platform}\""
            : "\"@RG\\tID:${sampleLaneId}\\t${cn}PU:${meta.lane}\\tSM:${meta.patient}_${meta.sample}\\tLB:${meta.sample}\\tDS:${params.fasta}\\tPL:${params.seq_platform}\""

        // Create new meta map removing 'lane' and adding 'read_group' and 'sample_lane_id'
        Map newMeta = new LinkedHashMap(meta)
        newMeta.remove('lane')
        newMeta.put('read_group', readGroup.toString())
        newMeta.put('sample_lane_id', sampleLaneId.toString())

        return [newMeta, files]
    }

}
