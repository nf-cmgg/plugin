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

import java.util.regex.Matcher

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import nextflow.exception.AbortOperationException

/**
 * Common utilities for parsing
 */
@Slf4j
@CompileStatic
class ParseHelper {

    static String sampleFromPath(String basePath) {
        Matcher match = (basePath =~ /^([^\._-]+)[\._-].*$/)
        if (match.find()) {
            return match.group(1)
        }
        String msg = "Cannot parse sample name from path: ${basePath}"
        throw new ParseException(msg)
    }

}

/**
 * Exception when parsing using the parsing utilities
 */
@CompileStatic
class ParseException extends AbortOperationException {

    ParseException(String message) {
        super(message)
    }

}
