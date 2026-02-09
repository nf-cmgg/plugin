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

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.script.dsl.Description

/**
 * Main configuration scope for the nf-cmgg plugin.
 */
@CompileDynamic
class DoneConfig implements ConfigScope {

    @ConfigOption
    @Description('Create a DONE file after successful pipeline execution.')
    Boolean enabled = false

    @ConfigOption
    @Description('Location to create the DONE file after successful pipeline execution.')
    Path location

    DoneConfig(Map config) {
        this.enabled = getBoolean(config?.enabled, 'cmgg.done.enabled')
        this.location = getPath(config?.location, 'cmgg.done.location')
    }

}
