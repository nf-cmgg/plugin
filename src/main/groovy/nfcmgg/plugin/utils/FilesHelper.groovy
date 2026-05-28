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
import org.yaml.snakeyaml.Yaml

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * Common utilities for file operations
 */
@Slf4j
@CompileDynamic
class FilesHelper {

    static void checkParent(Path path) {
        if (!path.parent.exists()) {
            path.parent.mkdirs()
        }
    }

    static List<Map<String, Object>> readSamplesheet(Path samplesheet) {
        if (!samplesheet.exists()) {
            log.warn("Could not read samplesheet: ${samplesheet.toUriString()}")
            return []
        }
        String extension = samplesheet.extension
        if (extension == 'yaml' || extension == 'yml') {
            // Parse YAML file
            return new Yaml().load(samplesheet.text) as List<Map<String, Object>>
        }
        switch (extension) {
            case 'csv':
                // Parse CSV file
                return samplesheet.splitCsv(
                    header:true, sep:',', strip:true, quote:'\"') as List<Map<String, Object>>
            case 'tsv':
                // Parse TSV file
                return samplesheet.splitCsv(
                    header:true, sep:'\t', strip:true, quote:'\"') as List<Map<String, Object>>
            case 'json':
                // Parse JSON file
                return new groovy.json.JsonSlurper().parseText(samplesheet.text) as List<Map<String, Object>>
        }
        log.warn("Unsupported samplesheet format: $extension")
        return []
    }

}
