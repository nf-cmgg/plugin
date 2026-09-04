# Worksheets

Worksheets tell the nf-cmgg plugin how to turn a pipeline run into downstream samplesheets.

During a Nextflow run, the plugin watches published files, attaches them (and optional metrics) to samples from the input samplesheet, then writes YAML samplesheets for other pipelines.

A worksheet is a YAML file. Built-in worksheets live under `src/main/resources/worksheets/`. The plugin picks the worksheet whose `name` matches `manifest.name` in the pipeline’s `nextflow.config`.

Enable samplesheet generation with:

```groovy
cmgg {
    samplesheets {
        enabled = true
        // optional: location = '/path/to/output'
    }
}
```

You can validate a worksheet against [`worksheet-schema.json`](../worksheet-schema.json). Built-in worksheets already point the YAML language server at that schema.

By adding the following line at the top of the worksheet, you can have the [Yaml VScode plugin](https://marketplace.cursorapi.com/items/?itemName=redhat.vscode-yaml) automatically validate the validity of your worksheet:

```
# yaml-language-server: $schema=https://raw.githubusercontent.com/nf-cmgg/plugin/main/worksheet-schema.json
```

## How it works

1. **Start** — The plugin loads and validates the matching worksheet and the pipeline’s input samplesheet (`params.input`).
2. **Per sample** — For each sample ID (`id_field`), it builds a data map from the `input` and `values` blocks.
3. **During the run** — When a file is published, its basename is matched against `output` patterns (first match wins) and `metrics` patterns (all matches apply). Matching paths and metric values are attached to the sample.
4. **Finish** — Each entry under `samplesheets` is evaluated. Samples that pass `include_func` (and optionally `filter_func`) are written as YAML. Samples that fail `filter_func` go to a `*_failed` samplesheet.

---

## File structure

| Key            | Required | Purpose                                                                     |
| -------------- | -------- | --------------------------------------------------------------------------- |
| `name`         | yes      | Pipeline name; must equal `manifest.name` in the `nextflow.config` file     |
| `id_field`     | yes      | Column in the **input** samplesheet used as the common identifier           |
| `input`        | yes      | Define how to fetch values from the input samplesheet                       |
| `values`       | no       | Extra or derived fields (constants or derived values based on input values) |
| `output`       | yes      | Regex patterns that capture published files                                 |
| `metrics`      | no       | Pull values from metrics files (e.g. MultiQC tables)                        |
| `samplesheets` | yes      | List of output samplesheets to generate and how to structure them           |

Minimal skeleton:

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/nf-cmgg/plugin/main/worksheet-schema.json
name: my-org/my-pipeline
id_field: sample

input:
  sample:

output:
  bam:
    pattern: '.*\.bam$'

samplesheets:
  - name: downstream_samplesheet.yaml
    fields:
      sample:
      bam:
```

---

## Top-level fields

### `name`

Must match the pipeline’s `manifest.name`. That is how the plugin selects which worksheet to use.

### `id_field`

Name of the column in the **input samplesheet** that identifies each sample. It must appear as a `source` in the `input` block, or as a key when `source` is omitted.

Example: if the samplesheet column is `samplename` but the field defined for the plugin is `sample`:

```yaml
id_field: samplename
input:
  sample:
    source: samplename
```

Published files are linked to samples when the file basename starts with that ID (longest matching identifier wins if several IDs match).

## `input`

Defines how columns from the pipeline input samplesheet become fields the rest of the worksheet can use.

Each key is the **plugin field name**, meaning that this is the key used later on to use this specific value. Options:

| Option        | Meaning                                                                                                              |
| ------------- | -------------------------------------------------------------------------------------------------------------------- |
| `source`      | Column name in the input samplesheet. Defaults to the key.                                                           |
| `default`     | Used when the column is missing or empty. Defaults to `null`.                                                        |
| `type`        | Cast to `string`, `integer`, `float`, or `boolean`. Default: `string`. CSV/TSV inputs are always strings until cast. |
| `description` | Documentation only; ignored at runtime.                                                                              |

```yaml
input:
  id: # same name as column "id"
  sample:
    source: samplename # remap samplename → sample
  tag:
    default: ""
  sample_type:
    default: DNA
  sex:
    default: U
  binsize:
    type: integer
```

After conversion, these fields are available as `input.<key>` in the `values` block functions, and later as `data.<key>` in the samplesheet block functions.

## `values`

Adds constant or computed fields based on input values. Each entry must set **either** `value` **or** `func` (not both).

| Option        | Meaning                                                                                                                                                                                           |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `value`       | Constant always set for every sample.                                                                                                                                                             |
| `func`        | Groovy expression. Use `input.<field>` for fields from `input`. Prefer safe navigation (`?.`) for missing values. Additionally it also has access to all parameters using the `params` structure. |
| `description` | Documentation only.                                                                                                                                                                               |

```yaml
values:
  strandedness:
    value: unknown
  binsize:
    func: |
      (input.organism?.toLowerCase() == 'mus musculus' || input.genome?.toLowerCase() == 'mm10')
      && ![100, 500].contains(input.binsize) ? 100 : input.binsize
  exomecnv_batch:
    func: 'input.library + "_" + input.sex'
```

`func` expressions run in a restricted Groovy environment. Keep them simple and defensive. See [SafeGroovy.groovy](../src/main/groovy/nfcmgg/plugin/utils/SafeGroovy.groovy) for a list of allowed methods.

## `output`

Maps published files to field names by matching the file **basename** with a regex.

| Option        | Meaning                                     |
| ------------- | ------------------------------------------- |
| `pattern`     | Regex against the full basename (required). |
| `description` | Documentation only.                         |

**Order matters.** The plugin uses the **first** matching pattern. Put more specific patterns before broader ones.

```yaml
output:
  snp_cram:
    pattern: 'snp_.*\.cram$'
  snp_crai:
    pattern: 'snp_.*\.cram\.crai$'
  cram:
    pattern: '.*\.cram$' # would also match snp_*.cram if listed first
  crai:
    pattern: '.*\.crai$'
  fastq_1:
    pattern: '.*R1_\d\d\d\.fastq\.gz$'
  fastq_2:
    pattern: '.*R2_\d\d\d\.fastq\.gz$'
```

If several files match the same field for one sample, they are kept as a list and expanded into multiple samplesheet rows when the samplesheet is written.

## `metrics`

Reads values from metrics files (for example MultiQC tables) and attaches them to samples.

Unlike `output`, **every** matching metric is applied (order does not matter).

| Option        | Meaning                                                               |
| ------------- | --------------------------------------------------------------------- |
| `pattern`     | Regex against the published path basename (required).                 |
| `subpath`     | Relative path inside a matched **directory**.                         |
| `filetype`    | `tsv` (default), `csv`, `json`, `yaml`, or `yml`.                     |
| `id`          | Column/key in the metrics file that identifies the sample (required). |
| `field`       | Column/key holding the metric value (required).                       |
| `description` | Documentation only.                                                   |

```yaml
metrics:
  yield:
    pattern: ".*_SAV_data$"
    subpath: "multiqc_bclconvert_bysample.txt"
    filetype: tsv
    id: Sample
    field: yield_
```

Here, a published directory matching `.*_SAV_data$` is opened, `multiqc_bclconvert_bysample.txt` is read as TSV, and each row’s `yield_` is stored under `data.yield` for the sample named in the `Sample` column.

## `samplesheets`

A list of samplesheets to write when the run finishes. Each entry can contain the following keywords:

| Option         | Meaning                                                                                                                                                                                                                                                         |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`         | Output filename; must end in `.yaml` or `.yml` (required).                                                                                                                                                                                                      |
| `description`  | Documentation only.                                                                                                                                                                                                                                             |
| `include_func` | Groovy boolean. If omitted, all samples are candidates. Use `data.<field>` to fetch values defined in the `input`, `values`, `output` and `metrics` blocks. Additionally it also has access to all parameters using the `params` structure.                     |
| `filter_func`  | Groovy boolean for QC thresholds. Failures go to `<basename>_failed.<ext>`. Use `data.<field>` to fetch values defined in the `input`, `values`, `output` and `metrics` blocks. Additionally it also has access to all parameters using the `params` structure. |
| `fields`       | Columns of the output YAML (required).                                                                                                                                                                                                                          |

### Field mappings

Keys under `fields` are column names in the **output** samplesheet:

| Option   | Meaning                                                        |
| -------- | -------------------------------------------------------------- |
| `source` | Plugin data field to take the value from. Defaults to the key. |
| `type`   | Cast to `string`, `integer`, `float`, or `boolean`.            |

`null` values are omitted from the written samplesheet.

```yaml
samplesheets:
  - name: nfcmgg_sampletracking_samplesheet.yaml
    description: Input for nf-cmgg/sampletracking
    include_func: |
      (data.organism?.toLowerCase() == 'homo sapiens' || data.genome?.toLowerCase() == 'grch38')
      && data.sample_type?.toLowerCase() == 'dna'
      && data.tag?.toLowerCase() in ['wes', 'wgs']
    fields:
      sample:
      pool:
        source: library
      sample_bam:
        source: cram
      sample_bam_index:
        source: crai
      sex:

  - name: nfcore_rnafusion_samplesheet.yaml
    include_func: |
      data.sample_type?.toLowerCase() == 'rna'
      && data.fastq_1 != null
    filter_func: |
      data.yield?.toLong() >= 1000000L
    fields:
      sample:
      fastq_1:
      fastq_2:
      strandedness:
      reads:
        source: yield
```

With `filter_func`, samples that fail are written to e.g. `nfcore_rnafusion_samplesheet_failed.yaml`.

In `include_func` and `filter_func`, `data` exposes everything defined in `input`, `values`, `output`, and `metrics`. Additionally it also has access to all parameters using the `params` structure.

## End-to-end example

The built-in worksheet for **nf-cmgg/preprocessing** ([`nfcmgg_preprocessing.yml`](../src/main/resources/worksheets/nfcmgg_preprocessing.yml)) shows the full pattern:

1. Remap and default input columns (`samplename` → `sample`, defaults for `tag`, `sex`, …).
2. Derive flags and batch keys (`normdup`, `nipt`, `exomecnv_batch`).
3. Capture CRAMs, FASTQs, and per-base beds from published basenames.
4. Read yield from MultiQC BCL Convert tables.
5. Emit several downstream samplesheets (sampletracking, rnafusion, vivar, exomecnv, smallvariants), each with its own inclusion rules.

A shortened illustration of that flow:

```yaml
name: nf-cmgg/preprocessing
id_field: samplename

input:
  sample:
    source: samplename
  tag:
    default: ""
  sample_type:
    default: DNA

values:
  strandedness:
    value: unknown

output:
  cram:
    pattern: '.*\.cram$'
  crai:
    pattern: '.*\.crai$'

samplesheets:
  - name: nfcmgg_smallvariants_samplesheet.yaml
    include_func: |
      data.sample_type?.toLowerCase() == 'dna'
      && data.tag?.toLowerCase() in ['wes', 'wgs']
    fields:
      sample:
      cram:
      crai:
```

For a sample `ABC123` with tag `wes` and published files `ABC123.cram` / `ABC123.cram.crai`, the plugin writes a YAML entry roughly like:

```yaml
- sample: ABC123
  cram: /path/to/outdir/ABC123.cram
  crai: /path/to/outdir/ABC123.cram.crai
```

## Writing Groovy safely

Expressions in `values.func`, `samplesheets.include_func`, and `samplesheets.filter_func` should tolerate missing data:

```groovy
// Prefer
data.organism?.toLowerCase() == 'homo sapiens'

// Avoid (NullPointerException if organism is null)
data.organism.toLowerCase() == 'homo sapiens'
```

Use `data.<field> != null` when a published file or metric must be present before including a sample.

## Adding a worksheet for a new pipeline

1. Create `src/main/resources/worksheets/<pipeline>.yml`.
2. Set `name` to the pipeline’s `manifest.name`.
3. Point the language server at the schema:

   ```yaml
   # yaml-language-server: $schema=https://raw.githubusercontent.com/nf-cmgg/plugin/main/worksheet-schema.json
   ```

4. Define `id_field`, `input`, `output`, and at least one `samplesheets` entry.
5. Rebuild/install the plugin (`make assemble` / `make install`) and run with `cmgg.samplesheets.enabled = true`.

Only one worksheet is loaded per run: the first resource file whose `name` matches the current pipeline.
