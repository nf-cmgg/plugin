/* groovylint-disable JUnitPublicNonTestMethod, MethodName, JUnitPublicProperty, MethodReturnTypeRequired */
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

import groovy.transform.CompileDynamic
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

/**
 * Unit tests for FastqUtils
 */
@CompileDynamic
class FastqUtilsTest extends Specification {

    @TempDir
    Path tempDir

    def "readFirstLineOfFastq should return first line of gzipped fastq"() {
        given:
        Path fastq = tempDir.resolve('test.fastq.gz')
        String content = '@SEQ_ID\nAGCT'
        GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(fastq))
        gzipOut.write(content.bytes)
        gzipOut.close()

        when:
        String line = FastqUtils.readFirstLineOfFastq(fastq)

        then:
        line == '@SEQ_ID'
    }

    def "flowcellLaneFromFastq should extract flowcell from 5-field header"() {
        given:
        Path fastq = tempDir.resolve('test.fastq.gz')
        // @<instrument>:<lane>:<tile>:<x-pos>:<y-pos>
        String content = '@INSTRUMENT:1:1101:1234:5678\nAGCT'
        GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(fastq))
        gzipOut.write(content.bytes)
        gzipOut.close()

        when:
        String flowcell = FastqUtils.flowcellLaneFromFastq(fastq)

        then:
        flowcell == 'INSTRUMENT'
    }

    def "flowcellLaneFromFastq should extract flowcell from 7-field header"() {
        given:
        Path fastq = tempDir.resolve('test.fastq.gz')
        // @<instrument>:<run number>:<flowcell ID>:<lane>:<tile>:<x-pos>:<y-pos>
        String content = '@INSTRUMENT:123:FLOWCELL_ID:1:1101:1234:5678\nAGCT'
        GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(fastq))
        gzipOut.write(content.bytes)
        gzipOut.close()

        when:
        String flowcell = FastqUtils.flowcellLaneFromFastq(fastq)

        then:
        flowcell == 'FLOWCELL_ID'
    }

    def "addReadgroupToMeta should add readgroup info"() {
        given:
        Path fastq1 = tempDir.resolve('sample_R1.fastq.gz')
        String content = '@INSTRUMENT:1:1101:1234:5678\nAGCT'
        GZIPOutputStream gzipOut1 = new GZIPOutputStream(Files.newOutputStream(fastq1))
        gzipOut1.write(content.bytes)
        gzipOut1.close()

        List<Path> files = [fastq1]
        Map meta = [id: 'sample1', sample: 'sample1', patient: 'patient1', lane: '1']
        Map params = [
            seq_center: 'CENTER',
            fasta: 'ref.fa',
            seq_platform: 'ILLUMINA',
            umi_read_structure: false
        ]

        when:
        List result = FastqUtils.addReadgroupToMeta(meta, files, params)
        Map newMeta = result[0]

        then:
        newMeta.sample_lane_id == 'INSTRUMENT.sample1.1'
        newMeta.read_group.contains('ID:INSTRUMENT.sample1.1')
        newMeta.read_group.contains('CN:CENTER')
        newMeta.read_group.contains('LB:sample1')
        !newMeta.containsKey('lane')
    }

    def "addReadgroupToMeta should handle missing flowcell"() {
        given:
        Path fastq1 = tempDir.resolve('sample_R1.fastq.gz')
        String content = '@BAD_HEADER\nAGCT'
        GZIPOutputStream gzipOut1 = new GZIPOutputStream(Files.newOutputStream(fastq1))
        gzipOut1.write(content.bytes)
        gzipOut1.close()

        List<Path> files = [fastq1]
        Map meta = [id: 'sample1', sample: 'sample1', patient: 'patient1', lane: '1']
        Map params = [
            seq_center: 'CENTER',
            fasta: 'ref.fa',
            seq_platform: 'ILLUMINA',
            umi_read_structure: false
        ]

        when:
        List result = FastqUtils.addReadgroupToMeta(meta, files, params)
        Map newMeta = result[0]

        then:
        newMeta.sample_lane_id == 'sample1.1'
        newMeta.read_group.contains('ID:sample1.1')
    }

    def "addReadgroupToMeta should fail on mismatched flowcells"() {
        given:
        Path fastq1 = tempDir.resolve('sample_R1.fastq.gz')
        String content1 = '@FC1:1:1101:1234:5678\nAGCT'
        GZIPOutputStream gzipOut1 = new GZIPOutputStream(Files.newOutputStream(fastq1))
        gzipOut1.write(content1.bytes)
        gzipOut1.close()

        Path fastq2 = tempDir.resolve('sample_R2.fastq.gz')
        String content2 = '@FC2:1:1101:1234:5678\nAGCT'
        GZIPOutputStream gzipOut2 = new GZIPOutputStream(Files.newOutputStream(fastq2))
        gzipOut2.write(content2.bytes)
        gzipOut2.close()

        List<Path> files = [fastq1, fastq2]
        Map meta = [id: 'sample1', sample: 'sample1', patient: 'patient1', lane: '1']
        Map params = [:]

        when:
        FastqUtils.addReadgroupToMeta(meta, files, params)

        then:
        thrown(IllegalStateException)
    }

}
