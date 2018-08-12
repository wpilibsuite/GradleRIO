package edu.wpi.first.gradlerio.wpi

import edu.wpi.first.gradlerio.wpi.dependencies.WPIJavaDeps
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeDeps
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolsPlugin
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.logging.text.StyledTextOutput

@CompileStatic
class WPIPlugin implements Plugin<Project> {
    ETLogger logger

    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)
        logger = ETLoggerFactory.INSTANCE.create(this.class.simpleName)

        project.pluginManager.apply(WPIJavaDeps)
        project.pluginManager.apply(WPIToolsPlugin)

        project.plugins.withType(ToolchainsPlugin).all {
            logger.info("DeployTools Native Project Detected".toString())
            project.pluginManager.apply(WPINativeDeps)
            project.pluginManager.apply(WPIToolchainPlugin)
        }

        project.tasks.register("wpi") { Task task ->
            task.group = "GradleRIO"
            task.description = "Print all versions of the wpi block"
            task.doLast {
                wpiExtension.versions().each { String key, Tuple tup ->
                    println "${tup.first()}: ${tup[1]} (${key})"
                }
            }
        }
    }
}
