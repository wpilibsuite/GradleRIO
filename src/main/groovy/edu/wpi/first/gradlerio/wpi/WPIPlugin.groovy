package edu.wpi.first.gradlerio.wpi

import edu.wpi.first.gradlerio.wpi.dependencies.WPIDependenciesPlugin
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeDepRules
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeJsonDepRules
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolsPlugin
import edu.wpi.first.toolchain.ToolchainPlugin
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin
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

        project.pluginManager.apply(WPIToolsPlugin)
        project.pluginManager.apply(WPIDependenciesPlugin)

        project.plugins.withType(ToolchainsPlugin).all {
            logger.info("DeployTools Native Project Detected".toString())
            project.pluginManager.apply(WPINativeDepRules)
            project.pluginManager.apply(ToolchainPlugin)
            project.pluginManager.apply(RoboRioToolchainPlugin)
            project.pluginManager.apply(WPINativeCompileRules)
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
            addMavenRepositories(project, wpiExtension)
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

    void addMavenRepositories(Project project, WPIExtension wpi) {
        if (wpi.maven.useLocal) {
            project.repositories.maven { MavenArtifactRepository repo ->
                repo.name = "WPILocal"
                repo.url = "${project.extensions.getByType(WPIExtension).getFrcHome()}/maven"
            }
        }

        if (wpi.maven.useFrcMavenLocalDevelopment) {
            project.repositories.maven { MavenArtifactRepository repo ->
                repo.name = "FRCDevelopmentLocal"
                repo.url = "${System.getProperty('user.home')}/releases/maven/development"
            }
        }

        if (wpi.maven.useFrcMavenLocalRelease) {
            project.repositories.maven { MavenArtifactRepository repo ->
                repo.name = "FRCReleaseLocal"
                repo.url = "${System.getProperty('user.home')}/releases/maven/release"
            }
        }

        def sortedMirrors = wpi.maven.sort { it.priority }

        // If enabled, the development branch should have a higher weight than the release
        // branch.
        if (wpi.maven.useDevelopment) {
            sortedMirrors.each { WPIMavenRepo mirror ->
                if (mirror.development != null)
                    project.repositories.maven { MavenArtifactRepository repo ->
                        repo.name = "WPI${mirror.name}Development"
                        repo.url = mirror.development
                    }
            }
        }

        sortedMirrors.each { WPIMavenRepo mirror ->
            if (mirror.release != null)
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "WPI${mirror.name}Release"
                    repo.url = mirror.release
                }
        }
    }

    static boolean _beta_warn = false;
}
