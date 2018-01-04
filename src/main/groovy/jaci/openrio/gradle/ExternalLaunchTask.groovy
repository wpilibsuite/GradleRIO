package jaci.openrio.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem

import java.util.stream.Collectors

class ExternalLaunchTask extends DefaultTask {

    private def _withBuilderClosures = [] as List<Closure>
    def environment = [:] as Map<String, String>
    def persist = false
    def workingDir = null as File

    Process launch(String... cmd) {
        return this.launch(cmd as List<String>)
    }

    Process launch(List<String> cmd) {
        List<String> cmdWindows = ["cmd", "/c", "start"] + cmd
        List<String> cmdUnix = cmd

        String fileContent = ""
        if (OperatingSystem.current().isWindows()) {
            fileContent += "@echo off\nsetlocal\n"
        }
        environment.each { String key, String value ->
            if (OperatingSystem.current().isWindows()) {
                fileContent += "set ${key}=${value}\n"
            } else {
                fileContent += "export ${key}=${value}\n"
            }
        }

        if (workingDir != null) {
            workingDir.mkdirs()
            fileContent += "pushd ${workingDir.absolutePath}\n"
        }

        def ccmd = cmd
        String first = ccmd.remove(0)
        fileContent += "${first + " " + ccmd.collect { "\"${it}\"" }.join(" ")}\n"

        if (workingDir != null) {
            fileContent += "popd\n"
        }

        if (OperatingSystem.current().isWindows()) {
            fileContent += "endlocal\n"
        }

        File file = new File(project.buildDir, "gradlerio_${name}.${OperatingSystem.current().isWindows() ? "bat" : "sh"}")
        file.text = fileContent

        if (project.hasProperty('headless')) {
            println "Commands written to ${file.absolutePath}! Run this file."
            return null;
        } else {
            ProcessBuilder builder
            if (OperatingSystem.current().isWindows()) {
                builder = new ProcessBuilder("cmd", "/c", "start", file.absolutePath)
            } else {
                builder = new ProcessBuilder(file.absolutePath)
            }
            return builder.start()
        }
    }
}
