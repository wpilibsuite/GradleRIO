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
        def toolConfig = project.configurations.maybeCreate("wpiTools")

        def wpi = project.extensions.getByType(WPIExtension)
        project.afterEvaluate {
            List<WPITool> tools = []
            def toolFolder = 'C:\\Users\\Public\\frc2018\\tools'
            tools << new WPITool(project, "SmartDashboard", wpi.smartDashboardVersion, "edu.wpi.first.wpilib:SmartDashboard:${wpi.smartDashboardVersion}", toolFolder)
            tools << new WPITool(project, "ShuffleBoard", wpi.shuffleboardVersion, "edu.wpi.first.shuffleboard:app:${wpi.shuffleboardVersion}", toolFolder)

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
