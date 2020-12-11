package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.SingletonTask
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

import javax.inject.Inject

@CompileStatic
class ToolRunTask extends DefaultTask implements SingletonTask {
    @Internal
    TaskProvider<ToolInstallTask> installTask
    @Internal
    String toolName

    @Inject
    ToolRunTask(String name, TaskProvider<ToolInstallTask> installTask) {
        group = 'GradleRIO'
        description = "Run the tool $name"

        this.toolName = name
        if (installTask != null) {
            this.installTask = installTask
            dependsOn(installTask)
        }
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
        ProcessBuilder builder = new ProcessBuilder('wscript.exe', outputFile.absolutePath, 'silent')
        Process proc = builder.start()
        int result = proc.waitFor()
        if (result != 0) {
            def stdOut = proc.getInputStream().text;
            def stdErr = proc.getErrorStream().text;
            throw new ToolRunException(stdOut, stdErr)
        }
    }

    void runToolUnix() {
        def iTask = installTask.get()
        def outputFile = new File(ToolInstallTask.toolsFolder, iTask.toolName + '.py')
        project.exec { ExecSpec spec ->
            spec.executable = outputFile.absolutePath
        }
    }

    @Override
    String singletonName() {
        return toolName
    }
}
