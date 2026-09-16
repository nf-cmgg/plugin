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
package nfcmgg.plugin.config

import static nfcmgg.plugin.utils.ConfigTypeChecker.getBoolean
import static nfcmgg.plugin.utils.ConfigTypeChecker.getPath

import java.nio.file.Path

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.script.dsl.Description
import nextflow.Nextflow

/**
 * Main configuration scope for the nf-cmgg plugin.
 */
@CompileDynamic
@Slf4j
class SamplesheetsConfig implements ConfigScope {

    @ConfigOption
    @Description('Configuration scope for the creation of samplesheet files after successful pipeline execution.')
    Boolean enabled = false

    @ConfigOption
    @Description('Location to create the samplesheet files after successful pipeline execution.')
    Path location

    @ConfigOption
    @Description('A list of worksheets to use for the automatic samplesheet generation.')
    List<Path> worksheets = []

    SamplesheetsConfig(Map config) {
        this.enabled = getBoolean(config?.enabled, 'cmgg.samplesheets.enabled')
        this.location = getPath(config?.location, 'cmgg.samplesheets.location')
        if (config?.worksheets in List) {
            worksheets = config.worksheets.collect { sheet ->
                Path worksheetPath = getPath(sheet, 'cmgg.samplesheets.worksheets')
                if (!worksheetPath.exists()) {
                    log.error("Worksheet '${sheet}' does not exist, skipping...")
                    return null
                }
                return worksheetPath
            }.findAll { sheet -> sheet != null }
            }
        ((Path) Nextflow.file(getClass().getResource('/worksheets').toURI().toString())).eachFile { res ->
            worksheets << res
        }
        }

    }
