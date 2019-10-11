package edu.wpi.first.gradlerio

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class JsonMergeTask extends DefaultTask implements SingletonTask {

    @Internal
    String singletonName = "jsonMerge"

    @InputDirectory
    File folder

    @OutputFile
    File out

    @TaskAction
    void merge() {
        def containerFolder = folder
        def outfile = out

        if (containerFolder.exists()) {
            def files = containerFolder.listFiles().findAll {
                it.isFile() && it.name.endsWith(".json") && it.absolutePath != outfile.absolutePath
            } as List<File>
            JsonUtil.mergeArrays(files, outfile)
        }
    }

    @Override
    String singletonName() {
        return singletonName
    }
}
