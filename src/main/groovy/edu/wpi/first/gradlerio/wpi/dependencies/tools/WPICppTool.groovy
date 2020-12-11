package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class WPICppTool {
    //TaskProvider<CppToolInstallTask> toolInstallTask
    TaskProvider<CppToolRunTask> toolRunTask

    String name

    String version

    WPICppTool(Project project, String name, String version, String artifactId) {
        def config = project.configurations.getByName('wpiCppTools')
        String toolsClassifier = project.extensions.getByType(WPIExtension).cppToolsClassifier
        artifactId += ":${toolsClassifier}@zip"
        def dependency = project.dependencies.add("wpiCppTools", artifactId)
        //toolInstallTask = project.tasks.register("${name}Install".toString(), CppToolInstallTask, name, config, dependency)
        toolRunTask = project.tasks.register(name, CppToolRunTask, name)
        this.name = name
        this.version = version
    }
}
