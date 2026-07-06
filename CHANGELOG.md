## 0.2.7

1. Rnafusion samplesheets will now contain multiple entries of the same sample when fastq files from multiple lanes are created by nf-cmgg/preprocessing

## 0.2.6

1. Convert the fastq yield to a Long type instead of Integer to prevent integer overflows

## 0.2.5

1. `null` values won't be put in the samplesheet but will throw a warning instead
2. Add mouse data and remove mitochondrial data from the vivar samplesheet

## 0.2.4

1. Fixed samplesheets being empty in 0.2.3
2. Added a filter for rnafusion samplesheets. Samples with less than 1 million reads will now be put in a `nfcore_rnafusion_samplesheet_failed.yaml` file

## 0.2.3

Added `tissue` samples to the vivar samplesheet

## 0.2.2

Removed the `sex` field and added the `proband` field to the vivar samplesheet

## 0.2.1

Removed the filter step on `purpose` fields in the preprocssing samplesheets

## 0.2.0

This release adds full support for all diagnostic pipelines used until now

## 0.1.0

The first release of the plugin
