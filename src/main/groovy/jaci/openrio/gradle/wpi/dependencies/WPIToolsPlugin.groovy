package jaci.openrio.gradle.wpi.dependencies

import groovy.transform.CompileStatic
import jaci.openrio.gradle.ExternalLaunchTask
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class WPIToolsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.configurations.maybeCreate("wpiTools")

        def wpi = project.extensions.getByType(WPIExtension)
        project.dependencies.add("wpiTools", "edu.wpi.first.wpilib:SmartDashboard:${wpi.smartDashboardVersion}")
        project.dependencies.add("wpiTools", "edu.wpi.first.shuffleboard:Shuffleboard:${wpi.shuffleboardVersion}")

        smartDashboardDirectory().mkdirs()
        shuffleboardDirectory().mkdirs()

        def jvm = OperatingSystem.current().isWindows() ? "java" : Jvm.current().getExecutable("java").absolutePath

        project.tasks.create("smartDashboard", ExternalLaunchTask) { ExternalLaunchTask task ->
            task.group = "GradleRIO"
            task.description = "Launch Smart Dashboard"

            task.doLast {
                println "NOTE: SmartDashboard is old, and is depreciated in 2018 and beyond! Use Shuffleboard instead! Run ./gradlew shuffleboard."

                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "SmartDashboard" })
                task.workingDir = smartDashboardDirectory()
                task.launch(jvm, "-jar", jarfiles.first().absolutePath)
            }
        }

        project.tasks.create("shuffleboard", ExternalLaunchTask) { ExternalLaunchTask task ->
            task.group = "GradleRIO"
            task.description = "Launch Shuffleboard"

            task.doLast {
                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "Shuffleboard" })
                task.workingDir = shuffleboardDirectory()
                task.launch(jvm, "-jar", jarfiles.first().absolutePath)
            }
        }
    }

    public static File smartDashboardDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "SmartDashboard")
    }

    public static File shuffleboardDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "Shuffleboard")
    }
}
