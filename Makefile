GRADLE := $(shell command -v gradle 2>/dev/null || echo ./gradlew)

# Build the plugin
assemble:
	$(GRADLE) assemble

clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	$(GRADLE) clean

# Run tests with mock pipelines
test:
	$(GRADLE) install && nf-test test

# Run tests with mock pipelines and update the snapshots
test-update:
	$(GRADLE) install && nf-test test --update-snapshot

# Install the plugin into local nextflow plugins dir
install:
	$(GRADLE) install

# Publish the plugin
release:
	$(GRADLE) releasePlugin

# Lint the groovy code
lint:
	npm-groovy-lint "src/**/*.groovy" --fix && npm-groovy-lint "src/**/*.groovy" --format