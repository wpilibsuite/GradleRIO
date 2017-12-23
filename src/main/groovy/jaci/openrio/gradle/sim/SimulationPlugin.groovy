package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.XmlProvider
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.ide.visualstudio.VisualStudioExtension
import org.gradle.ide.visualstudio.VisualStudioProject
import org.gradle.internal.os.OperatingSystem
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinary
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryTasks
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
        return rtLibs.join(OperatingSystem.current().isWindows() ? ";" : ":")
    }

    static class SimRules extends RuleSource {
        @Mutate
        void createInstallAllComponentsTask(ModelMap<Task> tasks, ComponentSpecContainer components) {
            components.withType(NativeExecutableSpec).each { NativeExecutableSpec spec ->
                spec.binaries.withType(NativeExecutableBinarySpec).each { NativeExecutableBinarySpec bin ->
                    if (bin.targetPlatform.operatingSystem.current) {
                        if (!tasks.containsKey("simulate${spec.name.capitalize()}"))
                            tasks.create("simulate${spec.name.capitalize()}", SimulationTask) { SimulationTask task ->
                                task.group = "GradleRIO"
                                task.description = "Simulate Task for ${spec.name} executable"

                                task.dependsOn(bin.tasks.withType(InstallExecutable))
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
