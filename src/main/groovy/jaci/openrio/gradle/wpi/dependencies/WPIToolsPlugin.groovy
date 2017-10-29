package jaci.openrio.gradle.wpi.dependencies

import groovy.transform.CompileStatic
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

        project.task("smartDashboard") { Task task ->
            task.group = "GradleRIO"
            task.description = "Launch Smart Dashboard"

            task.doLast {
                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "SmartDashboard" })
                ProcessBuilder builder
                if (OperatingSystem.current().isWindows()) {
                    builder = new ProcessBuilder(
                        "cmd", "/c", "start",
                        "java", "-jar", "${jarfiles.first().absolutePath}".toString()
                    )
                } else {
                    builder = new ProcessBuilder(
                        Jvm.current().getExecutable("java").absolutePath,
                        "-jar",
                        jarfiles.first().absolutePath
                    )
                }

                builder.directory(smartDashboardDirectory())
                builder.start()
            }
        }

        project.task("shuffleboard") { Task task ->
            task.group = "GradleRIO"
            task.description = "Launch Shuffleboard"

            task.doLast {
                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "Shuffleboard" })
                ProcessBuilder builder
                if (OperatingSystem.current().isWindows()) {
                    builder = new ProcessBuilder(
                            "cmd", "/c", "start",
                            "java", "-jar", "${jarfiles.first().absolutePath}".toString()
                    )
                } else {
                    builder = new ProcessBuilder(
                            Jvm.current().getExecutable("java").absolutePath,
                            "-jar",
                            jarfiles.first().absolutePath
                    )
                }

                builder.directory(shuffleboardDirectory())
                builder.start()
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
