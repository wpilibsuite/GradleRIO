package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class WPITool {
    final TaskProvider<ToolInstallTask> toolInstallTask
    final TaskProvider<ToolRunTask> toolRunTask

    final Configuration configuration
    final Dependency dependency

    final String name
    final String version
    final String installTaskName

    WPITool(Project project, String name, String version, String artifactId, boolean platformJars) {
        configuration = project.configurations.getByName("wpiTools")
        String toolsClassifier = project.extensions.getByType(WPIExtension).toolsClassifier
        if (platformJars) {
            artifactId += ":${toolsClassifier}"
        }
        dependency = project.dependencies.add("wpiTools", artifactId)
        installTaskName = "${name}Install".toString()
        toolInstallTask = project.tasks.register(installTaskName, ToolInstallTask, this)
        toolRunTask = project.tasks.register(name, ToolRunTask, name, toolInstallTask)
        this.name = name
        this.version = version
    }

    WPIToolInfo getWPIToolInfo() {
        def toolInfo = new DefaultWPIToolInfo()
        toolInfo.name = name
        toolInfo.version = dependency.version
        toolInfo.installTaskName = installTaskName
        return toolInfo
    }
}
