package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.ide.visualstudio.VisualStudioExtension
import org.gradle.ide.visualstudio.VisualStudioProject
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
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

        project.tasks.withType(Jar).all { Jar jarTask ->
            def attr = jarTask.manifest.attributes
            if (jarTask.name.equals("jar")) {   // TODO Make this configurable (for alternate jars)
                project.tasks.create("simulate${jarTask.name.capitalize()}", JavaSimulationTask) { JavaSimulationTask task ->
                    task.group = "GradleRIO"
                    task.description = "Simulate Task for Java/Kotlin/JVM"

                    task.jar = jarTask
                    null
                }
            }
        }
    }

    static String envDelimiter() {
        return OperatingSystem.current().isWindows() ? ";" : ":"
    }

    static String getHALExtensionsEnvVar(Project project) {
        def cfg = project.configurations.getByName("simulation")
        def ext = OperatingSystem.current().sharedLibrarySuffix
        def rtLibs = []
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
                rtLibs += f
            }
        }
        return rtLibs.join(envDelimiter())
    }

    static class SimRules extends RuleSource {
        @Mutate
        void createSimulateComponentsTask(ModelMap<Task> tasks, ComponentSpecContainer components, ExtensionContainer extCont) {
            def project = extCont.getByType(GradleRIOPlugin.ProjectWrapper).project
            components.withType(NativeExecutableSpec).each { NativeExecutableSpec spec ->
                spec.binaries.withType(NativeExecutableBinarySpec).each { NativeExecutableBinarySpec bin ->
                    if (bin.targetPlatform.operatingSystem.current && !bin.targetPlatform.name.equals('roborio')) {
                        tasks.create("simulate${spec.name.capitalize()}${bin.targetPlatform.name.capitalize()}", NativeSimulationTask) { NativeSimulationTask task ->
                            task.group = "GradleRIO"
                            task.description = "Simulate Task for ${spec.name} native executable"

                            // TODO This needs _something_ since we can't make it depend on InstallExecutable
                            // This imposes a limit where if a source file is edited, the daemon will never kill the
                            // process. Same goes for the java above. Or does it?
                            task.binary = bin
                            null
                        }
                    }
                }
            }
        }

        @Mutate
        void createVisualStudioBindings(VisualStudioExtension vs, ExtensionContainer extCont) {
            def project = extCont.getByType(GradleRIOPlugin.ProjectWrapper).project
            vs.projects.all { VisualStudioProject pj ->
                if (pj.component instanceof NativeExecutableSpec) {
                    boolean shouldAddHALArgs = false
                    (pj.component as NativeExecutableSpec).binaries.withType(NativeExecutableBinarySpec).each { NativeExecutableBinarySpec bin ->
                        if (bin.targetPlatform.operatingSystem.current) {
                            shouldAddHALArgs = true
                        }
                    }

                    if (shouldAddHALArgs) {
                        new File(pj.projectFile.location.parentFile, "${pj.projectFile.location.name}.user").text =
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<Project ToolsVersion=\"15.0\" xmlns=\"http://schemas.microsoft.com/developer/msbuild/2003\">\n" +
                                "  <PropertyGroup Condition=\"'\$(Configuration)|\$(Platform)'=='any64|Win32'\">\n" +
                                "    <LocalDebuggerEnvironment>HALSIM_EXTENSIONS=${SimulationPlugin.getHALExtensionsEnvVar(project)}</LocalDebuggerEnvironment>\n" +
                                "    <DebuggerFlavor>WindowsLocalDebugger</DebuggerFlavor>\n" +
                                "  </PropertyGroup>\n" +
                                "</Project>"
                    }
                }
            }
        }
    }

}
