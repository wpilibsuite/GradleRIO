package jaci.openrio.gradle.wpi

import groovy.transform.CompileStatic
import jaci.openrio.gradle.wpi.dependencies.WPIJavaDeps
import jaci.openrio.gradle.wpi.dependencies.WPINativeDeps
import jaci.openrio.gradle.wpi.dependencies.WPIToolsPlugin
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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
