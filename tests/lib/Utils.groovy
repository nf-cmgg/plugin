import groovy.transform.CompileDynamic

import java.nio.file.Path

/**
  * A series of utility functions for testing.
  */
@CompileDynamic
class Utils {

    /* groovylint-disable-next-line FactoryMethodName */
    static List<String> create_yaml_snap(Path f, String outputDir) {
        return [f.toString().tokenize('/')[-1], f.text.replaceAll("${outputDir}/", '').tokenize('\n')]
    }

}