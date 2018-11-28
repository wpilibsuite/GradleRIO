package edu.wpi.first.gradlerio.wpi

import edu.wpi.first.gradlerio.wpi.dependencies.WPIJavaDeps
import edu.wpi.first.gradlerio.wpi.dependencies.WPIJsonDepsPlugin
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeDeps
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeJsonDepRules
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolsPlugin
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import edu.wpi.first.vscode.GradleVsCode
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.internal.logging.text.StyledTextOutput

@CompileStatic
class WPIPlugin implements Plugin<Project> {
    ETLogger logger

    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)
        logger = ETLoggerFactory.INSTANCE.create(this.class.simpleName)

        project.pluginManager.apply(WPIJavaDeps)
        project.pluginManager.apply(WPIToolsPlugin)
        project.pluginManager.apply(WPIJsonDepsPlugin)

        project.plugins.withType(ToolchainsPlugin).all {
            logger.info("DeployTools Native Project Detected".toString())
            project.pluginManager.apply(WPINativeDeps)
            project.pluginManager.apply(WPIToolchainPlugin)
            project.pluginManager.apply(GradleVsCode)
            project.pluginManager.apply(WPINativeJsonDepRules)
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

        project.tasks.register("explainRepositories") { Task task ->
            task.group = "GradleRIO"
            task.description = "Explain all Maven Repos present on this project"
            task.doLast {
                explainRepositories(project)
            }
        }

        // TODO: Remove for stable
        project.afterEvaluate {
            if (!_beta_warn) {
                def style = StyledTextOutput.Style.SuccessHeader
                logger.withLock {
                    logger.logStyle("NOTE: You are using a BETA version of GradleRIO, designed for the 2019 Season!", style)
                    logger.logStyle("This release requires the 2019 RoboRIO Image, and may be unstable. Do not use this for the official competition season.", style)
                    logger.logStyle("If you encounter any issues and/or bugs, please report them to https://github.com/wpilibsuite/GradleRIO", style)
                }
                _beta_warn = true;
            }
        }

        project.gradle.buildFinished {
            _beta_warn = false;
        }
    }

    void explainRepositories(Project project) {
        project.repositories.withType(MavenArtifactRepository).each { MavenArtifactRepository repo ->
            println("${repo.name} -> ${repo.url}")
        }
    }

    static boolean _beta_warn = false;
}
