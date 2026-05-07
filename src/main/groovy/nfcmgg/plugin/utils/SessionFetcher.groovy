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

import java.nio.file.Path

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import org.yaml.snakeyaml.Yaml

import nextflow.Session
import nextflow.Nextflow

/**
 * Common utilities for fetching session information
 */
@Slf4j
@CompileDynamic
class SessionFetcher {

    final private static String CREATIONDATE = new Date().format('yyyyMMdd_HHmmss')

    static Path getOutdir(Session session) {
        String outdirString = session?.params?.get('outdir', null)
        Path outdir = outdirString ? Nextflow.file(outdirString) as Path : null
        if (outdir == null) {
            outdir = session?.workDir
            log.warn("No outdir specified, using work directory at $outdir instead")
        }
        return outdir
    }

    static Path getSamplesheetOutdir(Session session) {
        return getOutdir(session).resolve("samplesheets/${CREATIONDATE}")
    }

    static List<Map<String, Object>> getInputSamplesheetList(Session session) {
        String samplesheetLocation = session?.params?.get('input', null)
        if (!samplesheetLocation) {
            return []
        }
        Path samplesheetPath = Nextflow.file(samplesheetLocation) as Path
        if (!samplesheetPath.exists()) {
            return []
        }
        String extension = samplesheetPath.extension
        if (extension == 'yaml' || extension == 'yml') {
            // Parse YAML file
            return new Yaml().load(samplesheetPath.text) as List<Map<String, Object>>
        }
        switch (extension) {
            case 'csv':
                // Parse CSV file
                return samplesheetPath.splitCsv(
                    header:true, sep:',', strip:true, quote:'\"') as List<Map<String, Object>>
            case 'tsv':
                // Parse TSV file
                return samplesheetPath.splitCsv(
                    header:true, sep:'\t', strip:true, quote:'\"') as List<Map<String, Object>>
            case 'json':
                // Parse JSON file
                return new groovy.json.JsonSlurper().parse(samplesheetPath.toFile()) as List<Map<String, Object>>
        }
        log.warn("Unsupported samplesheet format: $extension")
        return []
    }

}
