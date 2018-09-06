package edu.wpi.first.gradlerio.wpi.dependencies.tools

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class WPITool {
    TaskProvider<ToolInstallTask> toolInstallTask
    TaskProvider<ToolRunTask> toolRunTask

    String name

    String version

    WPITool(Project project, String name, String version, String artifactId, String extractonFolder) {
        def config = project.configurations.getByName("wpiTools")
        def dependency = project.dependencies.add("wpiTools", artifactId)
        toolInstallTask = project.tasks.register("${name}Install".toString(), ToolInstallTask, name, extractonFolder, config, dependency)
        toolRunTask = project.tasks.register(name, ToolRunTask, name, toolInstallTask)
        this.name = name
        this.version = version
    }
}
