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

import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.nio.file.Path

/**
 * Create samplesheets for pipelines after nf-core/rnafusion
 */
@Slf4j
@CompileStatic
class RnafusionObserver extends PipelineObserver {

    RnafusionObserver(Path location) {
        super(location)
    }

    @Override
    void onFlowComplete() {
        if (!session.success) { return }
        Path outdirPath
        Object outdirParam = session?.params?.outdir
        if (outdirParam in Path) {
            outdirPath = outdirParam as Path
        } else if (outdirParam in String) {
            outdirPath = Nextflow.file(outdirParam as String) as Path
        } else {
            log.warn('Cannot determine output directory from session parameters, skipping samplesheet generation')
            return
        }
        List<OutputEntry> entriesToDump = [new OutputEntry([
            run: outdirPath.baseName,
            outdir: outdirPath.toUriString()
        ])]
        creator.dump(
            entriesToDump,
            location.resolve('nfcmgg_report_samplesheet.yaml'),
        )
    }

}
