package edu.wpi.first.gradlerio.test

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import edu.wpi.first.gradlerio.JsonUtil
import edu.wpi.first.gradlerio.SingletonTask
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ExternalSimulationMergeTask extends DefaultTask implements SingletonTask {

    @TaskAction
    void merge() {
        def containerFolder = new File(project.rootProject.buildDir, "debug/partial")
        def outfile = new File(project.rootProject.buildDir, "debug/desktopinfo.json")

        if (containerFolder.exists()) {
            def files = containerFolder.listFiles().findAll {
                it.isFile() && it.name.endsWith(".json") && it.absolutePath != outfile.absolutePath
            } as List<File>
            JsonUtil.mergeArrays(files, outfile)
        }
    }

    @Override
    String singletonName() {
        return "mergeExternalSim"
    }
}
