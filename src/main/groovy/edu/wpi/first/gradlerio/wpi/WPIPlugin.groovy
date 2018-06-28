package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import edu.wpi.first.gradlerio.wpi.dependencies.WPIJavaDeps
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeDeps
import edu.wpi.first.gradlerio.wpi.dependencies.WPIToolsPlugin
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import jaci.gradle.IndentedLogger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.toolchain.VisualCpp
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.PlatformContainer

@CompileStatic
class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)

        project.pluginManager.apply(WPIJavaDeps)
        project.pluginManager.apply(WPINativeDeps)

        project.pluginManager.apply(WPIToolchainPlugin)

        project.pluginManager.apply(WPIToolsPlugin)

        project.task("wpi") { Task task ->
            task.group = "GradleRIO"
            task.description = "Print all versions of the wpi block"
            task.doLast {
                wpiExtension.versions().each { String key, Tuple tup ->
                    println "${tup.first()}: ${tup[1]} (${key})"
                }
            }
        }

        // TODO: Remove for stable, update for 2019 corelibs when appropriate
        project.afterEvaluate {
            def log = new IndentedLogger((project as ProjectInternal).services, 0);
            def style = StyledTextOutput.Style.SuccessHeader
            log.logStyle("NOTE: You are using an ALPHA version of GradleRIO, designed for the 2019 Season!", style)
            log.logStyle("This release uses the 2018 Core Libraries, however all tooling (GradleRIO + IDE support) is incubating for 2019", style)
            log.logStyle("If you encounter any issues and/or bugs, please report them to http://github.com/wpilibsuite/GradleRIO", style)
        }
    }

    static class WPIRules extends RuleSource {
        @Mutate
        void addPlatform(PlatformContainer platforms) {
            def roborio = platforms.maybeCreate('roborio', NativePlatform)
            roborio.architecture('arm')
            roborio.operatingSystem('linux')

            def desktop = platforms.maybeCreate('desktop', NativePlatform)
            System.getProperty("os.arch") == 'amd64' ? desktop.architecture('x86_64') : desktop.architecture('x86')

            def anyArm = platforms.maybeCreate('anyArm', NativePlatform)
            anyArm.architecture('arm')
        }

        @Mutate
        void addBinaryFlags(BinaryContainer binaries) {
            binaries.withType(NativeBinarySpec) { NativeBinarySpec bin ->
                if (!(bin.toolChain in VisualCpp)) {
                    bin.cppCompiler.args << "-std=c++1y" << '-g'
                } else {
                    bin.cppCompiler.args << '/Zi' << '/EHsc' << '/DNOMINMAX'
                    bin.linker.args << '/DEBUG:FULL'
                }
                null
            }
        }
    }
}
