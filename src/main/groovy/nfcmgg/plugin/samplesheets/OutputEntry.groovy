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

import groovy.transform.CompileDynamic
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

/**
 * A class representing an output entry from a pipeline.
 * It contains a map of key-value pairs, where the keys are the column names in the samplesheet
 * and the values are the corresponding values for that entry.
 */
@Slf4j
@Canonical
@CompileDynamic
class OutputEntry {

    final private Map<String, Object> values

    OutputEntry(Map<String, Object> defaultValues = [:]) {
        this.values = defaultValues
    }

    /*
    * Create a new entry containing only the specified keys.
    *
    * @param keys: a list of keys to keep in the new entry.
    *   If an element is a string, the key with the same name will be kept.
    *   If an element is a list, it should contain exactly 2 strings:
    *       the first is the original key, the second is the new key
    */
    OutputEntry subKeys(List<Object> keys) {
        Map<String, String> newValues = [:]
        keys.each { Object key ->
            String keyToCheck = key in String ? key as String : (key as List<String>)[0]
            if (!values.containsKey(keyToCheck) || values.get(keyToCheck) == null) {
                log.warn("Key '$keyToCheck' not found in entry")
                return
            }
            if (key in String) {
                newValues[key] = this.values[key]
            } else if (key in List<String> && key.size() == 2) {
                String oldKey = key[0]
                String newKey = key[1]
                newValues[newKey] = this.values[oldKey]
            }
        }
        OutputEntry newEntry = new OutputEntry(newValues)
        return newEntry
    }

    OutputEntry append(String key, Object value) {
        if (!this.values.containsKey(key)) {
            this.values[key] = []
        }
        this.values[key] << value
        return this
    }

    OutputEntry add(String key, Object value) {
        this.values[key] = value
        return this
    }

    String getAsString(String key) {
        return this.values.get(key)?.toString()
    }

    Object get(String key) {
        return this.values.get(key)
    }

    Object get(String key, Object defaultValue) {
        return this.values.get(key, defaultValue)
    }

    Map<String, Object> getValues() {
        return this.values
    }

}
