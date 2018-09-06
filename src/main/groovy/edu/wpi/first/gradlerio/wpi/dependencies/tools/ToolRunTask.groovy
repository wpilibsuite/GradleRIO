package edu.wpi.first.gradlerio.wpi.dependencies.tools

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

import javax.inject.Inject

@CompileStatic
class ToolRunTask extends DefaultTask {
    @Internal
    TaskProvider<ToolInstallTask> installTask

    @Inject
    ToolRunTask(String name, TaskProvider<ToolInstallTask> installTask) {
        group = 'GradleRIO'
        description = "Run the tool $name"

        this.installTask = installTask
        dependsOn(installTask)
    }

    @TaskAction
    void runTool() {
        def isWindows = OperatingSystem.current().isWindows()
        if (isWindows) {
            runToolWindows()
        } else {
            runToolUnix()
        }
    }

    void runToolWindows() {
        def iTask = installTask.get()
        def outputFile = new File(ToolInstallTask.toolsFolder, iTask.toolName + '.vbs')
        project.exec {
            def execer = (ExecSpec)it
            execer.executable = 'wscript.exe'
            execer.args outputFile
        }
    }

    void runToolUnix() {

    }
}
