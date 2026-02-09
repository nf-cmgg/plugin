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

/**
 * A class representing an output entry from a pipeline.
 * It contains a map of key-value pairs, where the keys are the column names in the samplesheet
 * and the values are the corresponding values for that entry.
 */
@Canonical
@CompileDynamic
class OutputEntry {

    final private Map<String, String> values

    OutputEntry(Map<String, String> defaultValues = [:]) {
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
            if (!values.containsKey(key)) {
                log.warn("Key '$key' not found in entry")
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

    OutputEntry add(String key, String value) {
        this.values[key] = value
        return this
    }

    Map<String, String> getValues() {
        return this.values
    }

}
