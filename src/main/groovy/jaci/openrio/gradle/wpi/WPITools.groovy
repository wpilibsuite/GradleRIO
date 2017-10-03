package jaci.openrio.gradle.wpi

import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.jvm.Jvm

class WPITools implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.configurations.maybeCreate("wpiTools")

        project.dependencies.add("wpiTools", "edu.wpi.first.wpilib:SmartDashboard:${project.wpi.smartDashboardVersion}")
        project.dependencies.add("wpiTools", "edu.wpi.first.wpilib:java-installer:${project.wpi.javaInstallerVersion}")

        smartDashboardDirectory().mkdirs()
        javaInstallerDirectory().mkdirs()

        project.task("smartDashboard") { Task task ->
            task.group = "GradleRIO"
            task.description = "Launch Smart Dashboard"

            task.doLast {
                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "SmartDashboard" })
                ProcessBuilder builder = new ProcessBuilder(
                        Jvm.current().getExecutable("java").absolutePath,
                        "-jar",
                        jarfiles.first().absolutePath)

                builder.directory(smartDashboardDirectory())
                builder.start()
            }
        }

        project.task("installJava") { Task task ->
            task.group = "GradleRIO"
            task.description = "Launch the Java Installer for the RoboRIO"

            task.doLast {
                def config = project.configurations.getByName("wpiTools")
                Set<File> jarfiles = config.files(config.dependencies.find { d -> d.name == "java-installer" })
                ProcessBuilder builder = new ProcessBuilder(
                        Jvm.current().getExecutable("java").absolutePath,
                        "-jar",
                        jarfiles.first().absolutePath)

                builder.directory(javaInstallerDirectory())
                builder.start()
            }
        }
    }

    public static File smartDashboardDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "SmartDashboard")
    }

    public static File javaInstallerDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "JavaInstaller")
    }
}
