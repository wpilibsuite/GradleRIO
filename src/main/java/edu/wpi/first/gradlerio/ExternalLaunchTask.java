package edu.wpi.first.gradlerio;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.ExecSpec;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class ExternalLaunchTask extends DefaultTask {
    private final Map<String, String> environment = new HashMap<>();
    private boolean scriptOnly = false;
    private final RegularFileProperty workingDir;

    @Input
    public Map<String, String> getEnvironment() {
        return environment;
    }

    @Internal
    public boolean isScriptOnly() {
        return scriptOnly;
    }

    public void setScriptOnly(boolean scriptOnly) {
        this.scriptOnly = scriptOnly;
    }

    @Internal
    public RegularFileProperty getWorkingDir() {
        return workingDir;
    }

    @Inject
    public ExternalLaunchTask(ObjectFactory objects) {
        workingDir = objects.fileProperty();
    }

    public Process launch(String... cmd) {
        return this.launch(Arrays.asList(cmd));
    }

    public Process launch(List<String> cmd) {
        SimulationExtension simExtension = getProject().getExtensions().getByType(SimulationExtension.class);

        String fileContent = "";
        if (OperatingSystem.current().isWindows()) {
            fileContent += "@echo off\nsetlocal\n";
        } else {
            fileContent += "#!/bin/bash\n\n";
        }
        // TODO finish me
        // environment.each { Map.Entry<String, String> entry ->
        //     if (OperatingSystem.current().isWindows()) {
        //         fileContent += "set ${entry.key}=${entry.value}\n"
        //     } else {
        //         fileContent += "export ${entry.key}=${entry.value}\n"
        //     }
        // }
        // simExtension.environment.each { Map.Entry<String, String> entry ->
        //     if (OperatingSystem.current().isWindows()) {
        //         fileContent += "set ${entry.key}=${entry.value}\n"
        //     } else {
        //         fileContent += "export ${entry.key}=${entry.value}\n"
        //     }
        // }

        // if (workingDir != null) {
        //     workingDir.mkdirs()
        //     fileContent += "pushd ${workingDir.absolutePath}\n"
        // }

        // fileContent += "${cmd.join(" ")}\n"

        // if (workingDir != null) {
        //     fileContent += "popd\n"
        // }

        // if (OperatingSystem.current().isWindows()) {
        //     fileContent += "endlocal\n"
        // }

        // File file = new File(project.buildDir, "gradlerio_${name}.${OperatingSystem.current().isWindows() ? "bat" : "sh"}")
        // project.buildDir.mkdirs()
        // file.text = fileContent

        // if (OperatingSystem.current().isUnix()) {
        //     project.exec { ExecSpec spec ->
        //         spec.commandLine "chmod"
        //         spec.args("0755", file.absolutePath)
        //     }
        // }

        // if (scriptOnly || project.hasProperty('headless')) {
        //     println "Commands written to ${file.absolutePath}! Run this file."
        //     return null;
        // } else {
        //     File stdoutFile = new File(project.buildDir, "stdout/${name}.log")
        //     ProcessBuilder builder
        //     if (OperatingSystem.current().isWindows()) {
        //         builder = new ProcessBuilder("cmd", "/c", "start",
        //                 "\"GradleRIO Simulation - ${project.name}\"", "\"${file.absolutePath}\"")
        //     } else {
        //         builder = new ProcessBuilder(file.absolutePath)
        //         stdoutFile.parentFile.mkdirs()
        //         builder.redirectOutput(stdoutFile)
        //         println "Program Output logfile: ${stdoutFile.absolutePath}"
        //     }
        //     Process process = builder.start()
        //     try {
        //         long pid = getPid(process);
        //         if (pid != -1) {
        //             File pidFile = new File(project.buildDir, "pids/${name}.pid")
        //             pidFile.parentFile.mkdirs()
        //             pidFile.text = pid.toString()
        //             println "Simulation Launched! PID: ${pid} (written to ${pidFile.absolutePath})"
        //         } else {
        //             println "Simulation Launched! PID Unknown (this JVM does not support java.lang.Process#pid)"
        //         }
        //     } catch (UnsupportedOperationException ex) {
        //         println "Simulation Launched! PID Unknown (${ex.class}: ${ex.message})"
        //     }
        //     return process
        // }
        return null;
    }

    public long getPid(Process process) {
        if (Jvm.current().getJavaVersion().isJava9Compatible()) {
            return process.pid();
        } else {
            return -1;
        }
    }
}
