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
package nfcmgg.plugin

import static nfcmgg.plugin.utils.FilesHelper.checkParent
import static nfcmgg.plugin.utils.SessionFetcher.getOutdir

import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserverV2


/**
 * Implements an observer that allows implementing custom
 * logic on nextflow execution events.
 */
@Slf4j
@CompileStatic
class DoneObserver implements TraceObserverV2 {

    private Session session
    private Path location

    DoneObserver(Path location) {
        this.location = location
    }

    @Override
    void onFlowCreate(Session session) {
        this.location = this.location ?: getOutdir(session)
        this.session = session
    }

    @Override
    void onFlowComplete() {
        if (session.success) {
            Path doneFile = location.resolve('DONE')
            checkParent(doneFile)
            doneFile.text = ''
        }
    }

}
