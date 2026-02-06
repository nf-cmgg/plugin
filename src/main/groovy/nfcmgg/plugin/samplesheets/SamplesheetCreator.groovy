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

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.introspector.Property

import java.nio.file.Path

/**
 * Create samplesheets for pipelines after nf-cmgg/preprocessing
 */
@Slf4j
@CompileStatic
class SamplesheetCreator {

    final private Yaml yaml

    SamplesheetCreator() {
        DumperOptions options = new DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.prettyFlow = true
        options.indent = 2
        Representer representer = new Representer(options) {

            @Override
            protected NodeTuple representJavaBeanProperty(
                Object javaBean,
                Property property,
                Object propertyValue,
                Tag customTag
            ) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null
                }
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
            }

        }
        representer.addClassTag(NfCmggPreprocessingOutputEntry, Tag.MAP)
        yaml = new Yaml(representer, options)
    }

    void dump(List<Object> entries, Path samplesheet) {
        samplesheet.text = yaml.dump(entries)
    }

}