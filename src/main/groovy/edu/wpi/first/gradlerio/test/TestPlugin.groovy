package edu.wpi.first.gradlerio.test

import groovy.transform.CompileStatic
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class TestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.configurations.maybeCreate("simulation")

        project.pluginManager.apply(JavaTestPlugin)

        project.plugins.withType(ToolchainsPlugin).all {
            project.pluginManager.apply(NativeTestPlugin)
        }

        project.tasks.register("externalSimulate", ExternalSimulationMergeTask, { ExternalSimulationMergeTask t ->
            t.dependsOn(project.tasks.withType(ExternalSimulationTask))
        } as Action<ExternalSimulationMergeTask>)
    }

    static List<String> getHALExtensions(Project project) {
        def cfg = project.configurations.getByName("simulation")
        def ext = OperatingSystem.current().sharedLibrarySuffix
        def allFiles = cfg.dependencies.collectMany({
            cfg.files(it)
        }) as Set<File>

        List<String> rtLibs = []

        allFiles.each { File f ->
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
        return rtLibs.join(envDelimiter())
    }

    static Map<String, String> getSimLaunchEnv(Project project, String ldpath) {
        def env = [:] as Map<String, String>
        env["HALSIM_EXTENSIONS"] = getHALExtensionsEnvVar(project)
        if (OperatingSystem.current().isUnix()) {
            env["LD_LIBRARY_PATH"] = ldpath
            env["DYLD_FALLBACK_LIBRARY_PATH"] = ldpath
            env["DYLD_LIBRARY_PATH"] = ldpath
        } else if (OperatingSystem.current().isWindows()) {
            env["PATH"] = System.getenv("PATH") + envDelimiter() + ldpath
        }
        return env
    }

    static String envDelimiter() {
        return OperatingSystem.current().isWindows() ? ";" : ":"
    }

}
