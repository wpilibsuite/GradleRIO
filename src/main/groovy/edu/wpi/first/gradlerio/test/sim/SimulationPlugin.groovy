package edu.wpi.first.gradlerio.test.sim

import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.test.ExtractTestJNITask
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.model.Validate
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.platform.base.ComponentSpecContainer

@CompileStatic
class SimulationPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configurations.maybeCreate("simulation")

        // Squash all the libraries into the actual library search path (same directory as
        // executable)
        // This really just protects against dumb zip extractions
        project.tasks.withType(InstallExecutable).all { InstallExecutable ietask ->
            def dest = new File(ietask.getInstallDirectory().get().asFile, "lib")
            ietask.doLast('extractLibsGrio') {
                project.copy { CopySpec copy ->
                    copy.into(dest)
                    copy.from(ietask.libs.files)
                }
            }
        }

        project.tasks.register("simulateExternalCpp", NativeExternalSimulationTask) { NativeExternalSimulationTask task ->
            task.group = "GradleRIO"
            task.description = "Simulate External Task for native executable"

            null
        }


        project.tasks.register("simulateExternalJava", JavaExternalSimulationTask) { JavaExternalSimulationTask task ->
            task.group = "GradleRIO"
            task.description = "Simulate External Task for Java/Kotlin/JVM"

            task.dependsOn("extractTestJNI")

            null
        }


        // TODO: This whole block needs to changed when Jar becomes Lazy, since otherwise the sim task will never get these bound.
        project.tasks.withType(Jar).configureEach { Jar jarTask ->
            project.tasks.named("simulateExternalJava").configure { JavaExternalSimulationTask t ->
                t.dependsOn(jarTask)
                t.jars << jarTask
            }

            if (jarTask.name.equals("jar")) {   // TODO Make this configurable (for alternate jars)
                project.tasks.register("simulate${jarTask.name.capitalize()}", JavaSimulationTask) { JavaSimulationTask task ->
                    task.group = "GradleRIO"
                    task.description = "Simulate Task for Java/Kotlin/JVM"

                    project.tasks.withType(ExtractTestJNITask).whenTaskAdded { ExtractTestJNITask extractTask ->
                        task.dependsOn extractTask
                    }

                    task.jar = jarTask
                    task.dependsOn jarTask

                    null
                }
            }
        }
    }

    static List<String> getHALExtensions(Project project) {
        def cfg = project.configurations.getByName("simulation")
        def ext = OperatingSystem.current().sharedLibrarySuffix
        List<String> rtLibs = []
        cfg.dependencies.collectMany {
            cfg.files(it)
        }.each { File f ->
            if (f.absolutePath.endsWith(".zip")) {
                rtLibs += (project.zipTree(f).matching { PatternFilterable pat ->
                    pat.include("**/*${ext}")
                }.files as Set<File>).collect { it.absolutePath }
            } else if (f.directory) {
                rtLibs += (project.fileTree(f).matching { PatternFilterable pat ->
                    pat.include("**/*${ext}")
                }.files as Set<File>).collect { it.absolutePath }
            } else {
                // Assume it's a native file already
                rtLibs += f.toString()
            }
        }
        return rtLibs
    }

    static String getHALExtensionsEnvVar(Project project) {
        def rtLibs = getHALExtensions(project)
        return rtLibs.join(TestPlugin.envDelimiter())
    }

    static class SimRules extends RuleSource {
        @Validate
        void createSimulateComponentsTask(ModelMap<Task> tasks, ComponentSpecContainer components, ExtensionContainer extCont) {
            NativeExternalSimulationTask mainTask = (NativeExternalSimulationTask)tasks.get('simulateExternalCpp')
            def project = extCont.getByType(GradleRIOPlugin.ProjectWrapper).project
            components.withType(NativeExecutableSpec).each { NativeExecutableSpec spec ->
                spec.binaries.withType(NativeExecutableBinarySpec).each { NativeExecutableBinarySpec bin ->
                    if (bin.targetPlatform.operatingSystem.current && !bin.targetPlatform.name.equals('roborio')) {
                        mainTask.binaries << bin
                        mainTask.dependsOn bin.tasks.install
                    }
                }
            }
        }
    }

}
