package edu.wpi.first.gradlerio

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

@CompileStatic
class ExternalLaunchTask extends DefaultTask {

    @Internal
    def environment = [:] as Map<String, String>
    @Internal
    boolean scriptOnly = false
    @Internal
    File workingDir = null as File

    Process launch(String... cmd) {
        return this.launch(cmd as List<String>)
    }

    Process launch(List<String> cmd) {
        String fileContent = ""
        if (OperatingSystem.current().isWindows()) {
            fileContent += "@echo off\nsetlocal\n"
        } else {
            fileContent += "#!/bin/bash\n\n"
        }
        environment.each { Map.Entry<String, String> entry ->
            if (OperatingSystem.current().isWindows()) {
                fileContent += "set ${entry.key}=${entry.value}\n"
            } else {
                fileContent += "export ${entry.key}=${entry.value}\n"
            }
        }

        if (workingDir != null) {
            workingDir.mkdirs()
            fileContent += "pushd ${workingDir.absolutePath}\n"
        }

        fileContent += "${cmd.join(" ")}\n"

        if (workingDir != null) {
            fileContent += "popd\n"
        }

        if (OperatingSystem.current().isWindows()) {
            fileContent += "endlocal\n"
        }

        File file = new File(project.buildDir, "gradlerio_${name}.${OperatingSystem.current().isWindows() ? "bat" : "sh"}")
        project.buildDir.mkdirs()
        file.text = fileContent

        if (OperatingSystem.current().isUnix()) {
            project.exec { ExecSpec spec ->
                spec.commandLine "chmod"
                spec.args("0755", file.absolutePath)
            }
        }

        if (scriptOnly || project.hasProperty('headless')) {
            println "Commands written to ${file.absolutePath}! Run this file."
            return null;
        } else {
            File stdoutFile = new File(project.buildDir, "stdout/${name}.log")
            ProcessBuilder builder
            if (OperatingSystem.current().isWindows()) {
                builder = new ProcessBuilder("cmd", "/c", "start", file.absolutePath)
            } else {
                builder = new ProcessBuilder(file.absolutePath)
                stdoutFile.parentFile.mkdirs()
                builder.redirectOutput(stdoutFile)
                println "Program Output logfile: ${stdoutFile.absolutePath}"
            }
            Process process = builder.start()
            try {
                long pid = process.pid()
                File pidFile = new File(project.buildDir, "pids/${name}.pid")
                pidFile.parentFile.mkdirs()
                pidFile.text = pid.toString()
                println "Simulation Launched! PID: ${pid} (written to ${pidFile.absolutePath})"
            } catch (UnsupportedOperationException ex) {
                println "Simulation Launched! PID Unknown."
            }
            return process
        }
    }
}
