# nf-cmgg plugin

## Setup

To use the plugin add the following to your `nextflow.config`:

```groovy
plugins {
   id 'nf-cmgg@0.1.0'
}
```

After this you should configure the plugin. See the [Configuration](#configuration) section for details.

## Configuration

The plugin can be configured by the `cmgg` configuration scope.

### Done

The `done` configuration scope allows you to configure the creation of a `DONE` file.

This scope contains the following options:

- `cmgg.done.enabled` (default: `false`): Whether to create a `DONE` file after successful pipeline execution.
- `cmgg.done.location` (default: `null`): The location where the `DONE` file will be created. If not set, the `DONE` file will be created in the pipeline's output directory. If the output directory is not set, the `DONE` file will be created in the current working directory.

### Samplesheets

The `samplesheets` configuration scope allows you to configure the generation of samplesheets.

This scope contains the following options:

- `cmgg.samplesheets.enabled` (default: `false`): Whether to generate samplesheets before pipeline execution.
- `cmgg.samplesheets.location` (default: `null`): The location where the samplesheets will be generated. If not set, the samplesheets will be generated in the pipeline's output directory. If the output directory is not set, the samplesheets will be generated in the current working directory.

## Building

To build the plugin:

```bash
make assemble
```

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `make install`
2. Run a pipeline with the plugin: `nextflow run hello -plugins nf-cmgg@0.1.0`

## Publishing

Plugins can be published to a central plugin registry to make them accessible to the Nextflow community.

Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where $HOME is your home directory. Add the following properties:

   - `npr.apiKey`: Your Nextflow Plugin Registry access token.

2. Use the following command to package and create a release for your plugin on GitHub: `make release`.

> [!NOTE]
> The Nextflow Plugin registry is currently available as preview technology.
