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
import groovy.transform.CompileStatic

import nextflow.Nextflow

/**
 * Common utilities for checking types in configuration files
 */
@Slf4j
@CompileStatic
class ConfigTypeChecker {

    static Map getMap(Object obj, String scope, Map defaultValue = [:]) {
        if (obj in Map || obj == null) {
            if (obj == null) {
                return defaultValue
            }
            return obj as Map
        }
        /* groovylint-disable-next-line LineLength */
        log.error("Expected a map for scope '$scope', but got an object of type ${obj.getClass().name}, defaulting to ${defaultValue}")
        return defaultValue
    }

    static String getString(Object obj, String scope, String defaultValue = '') {
        if (obj in String || obj == null) {
            if (obj == null) {
                return defaultValue
            }
            return obj as String
        }
        /* groovylint-disable-next-line LineLength */
        log.error("Expected a string for scope '$scope', but got an object of type ${obj.getClass().name}, defaulting to '${defaultValue}'")
        return defaultValue
    }

    static Boolean getBoolean(Object obj, String scope, Boolean defaultValue = false) {
        if (obj in Boolean || obj == null) {
            if (obj == null) {
                return defaultValue
            }
            return obj as Boolean
        }
        /* groovylint-disable-next-line LineLength */
        log.error("Expected a boolean for scope '$scope', but got an object of type ${obj.getClass().name}, defaulting to ${defaultValue}")
        return defaultValue
    }

    static Path getPath(Object obj, String scope, String defaultValue = null) {
        Path defaultPath = defaultValue ? Nextflow.file(defaultValue) as Path : null
        if (obj in String || obj == null) {
            if (obj == null) {
                return defaultPath
            }
            return Nextflow.file(obj as String) as Path
        }
        /* groovylint-disable-next-line LineLength */
        log.error("Expected a string for scope '$scope', but got an object of type ${obj.getClass().name}, defaulting to ${defaultValue}")
        return defaultPath
    }

}
