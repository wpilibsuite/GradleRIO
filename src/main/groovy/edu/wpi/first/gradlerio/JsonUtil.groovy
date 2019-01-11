package edu.wpi.first.gradlerio

import com.google.gson.GsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class JsonUtil {

    public static void mergeArrays(List<File> files, File outfile) {
        def slurper = new JsonSlurper()
        def merged = []

        for (File f : files) {
            merged.addAll(slurper.parse(f) as List)
        }

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        def json = gbuilder.create().toJson(merged)

        outfile.parentFile.mkdirs()
        outfile.text = json
    }

}
