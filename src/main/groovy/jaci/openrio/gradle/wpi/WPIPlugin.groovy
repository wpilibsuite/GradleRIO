package jaci.openrio.gradle.wpi

import jaci.openrio.gradle.wpi.dependencies.WPIDependenciesPlugin
import jaci.openrio.gradle.wpi.dependencies.WPIToolsPlugin
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

// TODO: Make @CompileStatic
class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)

        project.pluginManager.apply(WPIDependenciesPlugin)
        project.pluginManager.apply(WPIToolchainPlugin)
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