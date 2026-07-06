params {
    input: Path
}

workflow {
    main:
    def input_tmp = new groovy.yaml.YamlSlurper().parseText(params.input.text)
    def input = []
    input_tmp.each { entry ->
        input.add(entry)
        if (entry.containsKey('sample_info')) {
            input.addAll(new groovy.yaml.YamlSlurper().parseText(file(entry.sample_info).text))
        }
    }
    MOCK_OUTPUT(input)

    publish:
    out = MOCK_OUTPUT.out
}

output {
    out {
        path ""
    }
}

process MOCK_OUTPUT {
    input:
    val(input_list)

    output:
    path("*")

    script:
    def fastqs = input_list
        .findAll { entry -> entry.aligner == false }
        .collect { entry ->
            def lanes = entry.lanes?.toInteger() ?: 1
            def fastq_echos = (lanes > 1 ? 1..lanes : [1])
                .collect { lane ->
                    "echo '' | gzip > ${entry.samplename}_R1_00${lane}.fastq.gz && echo '' | gzip > ${entry.samplename}_R2_00${lane}.fastq.gz"
                }
            return fastq_echos
        }
        .flatten()
        .join("\n    ")
    def crams = input_list
        .findAll { entry -> entry.aligner instanceof String }
        .collect { entry -> "touch ${entry.samplename}.cram && touch ${entry.samplename}.cram.crai"}
        .join("\n    ")
    def snp_crams = input_list
        .findAll { entry -> entry.aligner instanceof String }
        .collect { entry -> "touch snp_${entry.samplename}.cram && touch snp_${entry.samplename}.cram.crai"}
        .join("\n    ")
    def per_base_beds = input_list
        .findAll { entry -> entry.aligner instanceof String }
        .collect { entry -> "echo '' | gzip > ${entry.samplename}.per-base.bed.gz && echo '' | gzip > ${entry.samplename}.per-base.bed.gz.csi"}
        .join("\n    ")
    def sav_data = input_list
        .collect { entry -> "echo '${entry.samplename}\t${entry.get('reads_to_use_in_test', '-1')}' >> multiqc_SAV_data/multiqc_bclconvert_bysample.txt"}
        .join("\n    ")
    """
    # Creating fastqs
    ${fastqs}

    # Creating crams
    ${crams}

    # Creating snp crams
    ${snp_crams}

    # Creating per base beds
    ${per_base_beds}

    # Creating SAV data
    mkdir multiqc_SAV_data
    echo "Sample\tyield_" > multiqc_SAV_data/multiqc_bclconvert_bysample.txt
    ${sav_data}
    """
}