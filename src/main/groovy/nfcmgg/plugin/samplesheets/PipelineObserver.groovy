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

import static nfcmgg.plugin.utils.ParseHelper.sampleFromPath
import static nfcmgg.plugin.utils.SessionFetcher.getSamplesheetOutdir
import static nfcmgg.plugin.utils.SessionFetcher.getInputSamplesheetList

import nfcmgg.plugin.worksheet.Worksheet
import nfcmgg.plugin.worksheet.WorksheetOutput
import nfcmgg.plugin.worksheet.WorksheetMetrics

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path

import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.event.FilePublishEvent

/**
 * A base observer class to be extended per pipeline
 */
@Slf4j
@CompileStatic
class PipelineObserver implements TraceObserverV2 {

    Map<String, OutputEntry> entries = new ConcurrentHashMap<>()

    // Location where the samplesheets should be generated
    Path location

    // The worksheet used to configure the samplesheet generation
    Worksheet worksheet

    // The input samplesheet converted to a List of maps
    List<Map<String, Object>> inputData

    // The nextflow session
    Session session

    // A set of all sample names
    Set<String> samples

    /**
     * Constructor for the PipelineObserver
     * @param location the location where the samplesheets should be generated,
     * if null it will be determined from the session
     */
    PipelineObserver(Path location, Worksheet worksheet) {
        this.location = location
        this.worksheet = worksheet
    }

    @Override
    void onFlowCreate(Session session) {
        this.location = this.location ?: getSamplesheetOutdir(session)
        this.inputData = getInputSamplesheetList(session)
        this.samples = inputData*.get(worksheet.idField).findAll { sample -> sample != null }.toSet() as Set<String>
        this.session = session
        log.info("Samplesheets will be generated in '$location'")
    }

    @Override
    void onFilePublish(FilePublishEvent event) {
        String targetName = event.target.name
        String targetPath = event.target.toUriString()

        WorksheetOutput.Field field = worksheet.output.matchingField(targetName)
        if (field) {
            String sample = safeGetSampleFromPath(targetName)
            entries[sample].append(field.key, targetPath)
        }

        List<WorksheetMetrics.Field> metrics = worksheet.metrics.matchingFields(targetName)
        metrics.each { metric ->
            Map<String, Object> metricData = metric.convert(targetPath)
            metricData.each { String sample, Object value ->
                addSampleIfMissing(sample)
                entries[sample].add(metric.key, value)
            }
        }
    }

    @Override
    void onFlowComplete() {
        worksheet.samplesheets.publishSamplesheets(entries, location)
    }

    /**
     * Override this method to provide default values for samples when they are first encountered
     * @param sample the sample name for which default values should be provided
     * @return a map of key-value pairs to be added to the sample entry when it is first created
     */
    /* groovylint-disable-next-line UnusedMethodParameter */
    private Map<String, Object> getDefaultValuesForSample(String sample) {
        Map<String,Object> sampleData = inputData.find { entry -> entry.get(worksheet.idField, '') == sample } ?: [:]
        Map<String, Object> inputMap = worksheet.input.convert(sampleData)
        return worksheet.values.convert(inputMap)
    }

    /**
     * Safely get the sample name for a given base path.
     * This method will try to find the best matching sample name from the input samplesheet based on the base path.
     * If no match is found, it will use the sample name extracted from the base path.
     * If multiple matches are found, it will use the longest match and log a warning.
     *
     * If the sample is not found in the pipeline entries,
     * the sample will be safely added to the entries using a set of default values fetched from the samplesheet.
     * These default values can be adjusted by overriding the `getDefaultValuesForSample` method.
     * @param basePath the base path for which to get the sample name
     * @return the sample name for the given base path
     */
    private String safeGetSampleFromPath(String inputBasePath) {
        String basePath = inputBasePath
        // Make sure SNP tracking data will be added to the correct sample
        if (basePath.startsWith('snp_')) {
            basePath = inputBasePath.replaceFirst('snp_', '')
        }
        List<String> possibleSamples = samples
            .findAll { sample -> basePath.startsWith(sample) }.toList() as List<String>
        String sample
        switch (possibleSamples.size()) {
            case 1:
                sample = possibleSamples[0]
                break
            case 0:
                sample = sampleFromPath(basePath)
                log.warn(
                    "Could not find sample for path '$basePath' in input samplesheet, using '$sample' as sample name"
                )
                break
            default:
                sample = possibleSamples.sort { a, b -> b.size() <=> a.size() }[0]
                log.warn(
                    /* groovylint-disable-next-line LineLength */
                    "Multiple possible samples found for path '$basePath': $possibleSamples, using '$sample' as sample name, because it is the longest match"
                )
        }
        addSampleIfMissing(sample)
        return sample
    }

    private void addSampleIfMissing(String sample) {
        entries.putIfAbsent(
            sample,
            /* groovylint-disable-next-line UnnecessaryCast */
            new OutputEntry(['id': sample] as Map<String, Object> + getDefaultValuesForSample(sample))
        )
    }

}
