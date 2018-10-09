package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class WPITool {
    TaskProvider<ToolInstallTask> toolInstallTask
    TaskProvider<ToolRunTask> toolRunTask

    Configuration configuration
    Dependency dependency

    String name

    String version

    WPITool(Project project, String name, String version, String artifactId, boolean platformJars) {
        configuration = project.configurations.getByName("wpiTools")
        String toolsClassifier = project.extensions.getByType(WPIExtension).toolsClassifier
        if (platformJars) {
            artifactId += ":${toolsClassifier}"
        }
        dependency = project.dependencies.add("wpiTools", artifactId)
        toolInstallTask = project.tasks.register("${name}Install".toString(), ToolInstallTask, this)
        toolRunTask = project.tasks.register(name, ToolRunTask, name, toolInstallTask)
        this.name = name
        this.version = version
    }

    ToolJson getToolJson() {
        def toolJson = new ToolJson()
        toolJson.name = name
        toolJson.version = dependency.version
        return toolJson
    }
}
