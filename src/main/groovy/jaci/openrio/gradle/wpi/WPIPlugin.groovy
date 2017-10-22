package jaci.openrio.gradle.wpi

import jaci.openrio.gradle.wpi.dependencies.WPIJavaDeps
import jaci.openrio.gradle.wpi.dependencies.WPINativeDeps
import jaci.openrio.gradle.wpi.dependencies.WPIToolsPlugin
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.nativeplatform.plugins.NativeComponentPlugin

// TODO: Make @CompileStatic
class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)

        project.pluginManager.apply(WPIJavaDeps)

        // Don't bother adding native stuff if we're not a native project
        project.plugins.matching { it instanceof NativeComponentPlugin }.all {
            project.pluginManager.apply(WPINativeDeps)
            project.pluginManager.apply(WPIToolchainPlugin)
        }

        project.pluginManager.apply(WPIToolsPlugin)

        project.task("wpi") { Task task ->
            task.group = "GradleRIO"
            task.description = "Print all versions of the wpi block"
            task.doLast {
                wpiExtension.versions().forEach { key, tup ->
                    println "${tup.first()}: ${tup[1]} (${key})"
                }
            }
        }
    }
}