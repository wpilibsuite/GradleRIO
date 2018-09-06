package edu.wpi.first.gradlerio.wpi.dependencies.tools

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

import javax.inject.Inject

@CompileStatic
class ToolInstallTask extends DefaultTask {
    @Internal
    String toolName
    @Internal
    Configuration configuration
    @Internal
    Dependency dependency
    @Internal
    String outputFolder

    @Inject
    ToolInstallTask(String toolName, String outputFolder, Configuration configuration, Dependency dep) {
        group = 'GradleRIO'
        description = "Install the tool $toolName"

        this.toolName = toolName
        this.configuration = configuration
        this.dependency = dep
        this.outputFolder = outputFolder
    }

    @TaskAction
    void installTool() {
        File jarfile = configuration.files(dependency).first()
        def of = new File(outputFolder)
        of.mkdirs()
        project.copy {
            def cp = (CopySpec)it
            cp.from jarfile
            cp.into of
            cp.rename {
                toolName + ".jar"
            }
        }
        if (OperatingSystem.current().isWindows()) {
            extractScript()
        }
    }

    void extractScript() {

        ToolInstallTask.class.getResourceAsStream('/ScriptBase.vbs').withCloseable {
            def outputFile = new File(outputFolder, toolName + '.vbs')
            outputFile.text = it.text
        }
    }
}
