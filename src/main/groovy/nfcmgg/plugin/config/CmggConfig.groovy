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

import static nfcmgg.plugin.utils.ConfigTypeChecker.getMap

import groovy.transform.CompileDynamic

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.config.spec.ScopeName
import nextflow.script.dsl.Description

/**
 * Main configuration scope for the nf-cmgg plugin.
 */
@CompileDynamic
@ScopeName('cmgg')
@Description('The `cmgg` scope allows you to configure the nf-cmgg plugin.')
class CmggConfig implements ConfigScope {

    @ConfigOption
    @Description('Configuration scope for the creation of `DONE` files after successful pipeline execution.')
    DoneConfig done

    @ConfigOption
    @Description('Configuration scope for the automatic generation of samplesheets for all supported pipelines.')
    SamplesheetsConfig samplesheets

    CmggConfig(Map config) {
        Map doneConfig = getMap(config?.done, 'cmgg.done')
        this.done = new DoneConfig(doneConfig)

        Map samplesheetsConfig = getMap(config?.samplesheets, 'cmgg.samplesheets')
        this.samplesheets = new SamplesheetsConfig(samplesheetsConfig)
    }

}
