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

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.plugin.extension.PluginExtensionPoint
import java.nio.file.Path

/**
 * Implements a custom function which can be imported by
 * Nextflow scripts.
 */
@CompileStatic
class CmggExtension extends PluginExtensionPoint {

    /*
     * Parse first line of a FASTQ file, return the flowcell id and lane number.
     */
    String flowcellLaneFromFastq(Path path) {
        return nfcmgg.plugin.utils.FastqUtils.flowcellLaneFromFastq(path)
    }

    /*
     * Get first line of a FASTQ file
     */
    String readFirstLineOfFastq(Path path) {
        return nfcmgg.plugin.utils.FastqUtils.readFirstLineOfFastq(path)
    }

    /*
     * Add readgroup to meta and remove lane
     */
    List addReadgroupToMeta(Map meta, List<Path> files, Map params) {
        return nfcmgg.plugin.utils.FastqUtils.addReadgroupToMeta(meta, files, params)
    }

    @Override
    protected void init(Session session) {
    }

}
