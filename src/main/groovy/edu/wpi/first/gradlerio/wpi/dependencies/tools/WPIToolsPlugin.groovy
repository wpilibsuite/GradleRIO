package edu.wpi.first.gradlerio.wpi.dependencies.tools

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

@CompileStatic
class WPIToolsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.configurations.maybeCreate("wpiTools")
        project.configurations.maybeCreate("wpiCppTools")

        def wpi = project.extensions.getByType(WPIExtension)
        project.afterEvaluate {
            List<WPITool> tools = []
            List<WPICppTool> cppTools = []

            def frcHome = wpi.frcHome
            String toolFolder = new File(frcHome, 'tools').toString()

            ToolInstallTask.toolsFolder = toolFolder
            tools << new WPITool(project, "SmartDashboard", wpi.smartDashboardVersion, "edu.wpi.first.tools:SmartDashboard:${wpi.smartDashboardVersion}", true)
            tools << new WPITool(project, "ShuffleBoard", wpi.shuffleboardVersion, "edu.wpi.first.shuffleboard:shuffleboard:${wpi.shuffleboardVersion}", true)
            tools << new WPITool(project, "OutlineViewer", wpi.outlineViewerVersion, "edu.wpi.first.tools:OutlineViewer:${wpi.outlineViewerVersion}", true)
            tools << new WPITool(project, "RobotBuilder", wpi.robotBuilderVersion, "edu.wpi.first.tools:RobotBuilder:${wpi.robotBuilderVersion}", false)
            tools << new WPITool(project, "RobotBuilder-Old", wpi.robotBuilderOldVersion, "edu.wpi.first.tools:RobotBuilder-Old:${wpi.robotBuilderOldVersion}", false)
            tools << new WPITool(project, "PathWeaver", wpi.pathWeaverVersion, "edu.wpi.first.tools:PathWeaver:${wpi.pathWeaverVersion}", true)
            cppTools << new WPICppTool(project, "Glass", wpi.glassVersion, "edu.wpi.first.tools:Glass:${wpi.glassVersion}")

            project.tasks.register("InstallAllTools") { Task task->
                task.group = 'GradleRIO'
                task.description = 'Install All Tools'

                tools.each { WPITool tool->
                    task.dependsOn tool.toolInstallTask
                }
            }
        }


    }
}
