package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class WPITool {
    TaskProvider<ToolInstallTask> toolInstallTask
    TaskProvider<ToolRunTask> toolRunTask

    String name

    String version

    WPITool(Project project, String name, String version, String artifactId, boolean platformJars) {
        def config = project.configurations.getByName("wpiTools")
        String toolsClassifier = project.extensions.getByType(WPIExtension).toolsClassifier
        if (platformJars) {
            artifactId += ":${toolsClassifier}"
        }
        def dependency = project.dependencies.add("wpiTools", artifactId)
        toolInstallTask = project.tasks.register("${name}Install".toString(), ToolInstallTask, name, config, dependency)
        toolRunTask = project.tasks.register(name, ToolRunTask, name, toolInstallTask)
        this.name = name
        this.version = version
    }
}
