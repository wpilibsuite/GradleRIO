package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import jaci.openrio.gradle.ExternalLaunchTask
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

import javax.inject.Inject

@CompileStatic
class JavaSimulationTask extends ExternalLaunchTask {

    Jar jar

    @TaskAction
    void run() {
        // Extract necessary libs
        def nativeLibs = project.configurations.getByName('nativeSimulationLib')
        def nativeZips = project.configurations.getByName('nativeSimulationZip')
        def extractionFiles = null as FileCollection

        nativeLibs.dependencies
                .matching { Dependency dep -> dep != null && nativeLibs.files(dep).size() > 0 }
                .all { Dependency dep ->
                    def fc = project.files(nativeLibs.files(dep).toArray())
                    if (extractionFiles == null) extractionFiles = fc
                    else extractionFiles += fc
                }

        nativeZips.dependencies
                .matching { Dependency dep -> dep != null && nativeZips.files(dep).size() > 0 }
                .all { Dependency dep ->
                    def ziptree = project.zipTree(nativeZips.files(dep).first())
                    ["**/*.so*", "**/*.so", "**/*.dll", "**/*.dylib"].collect { String pattern ->
                        def fc = ziptree.matching { PatternFilterable pat -> pat.include(pattern) }
                        if (extractionFiles == null) extractionFiles = fc
                        else extractionFiles += fc
                    }
                }

        def libdirs = extractionFiles.collect { File f -> f.parentFile }.unique()

        def env = SimulationPlugin.getHALExtensionsEnvVar(project)
        println "Using Environment: HALSIM_EXTENSIONS=${env}"
        def ldpath = libdirs.join(SimulationPlugin.envDelimiter())
        def java = OperatingSystem.current().isWindows() ? "java" : Jvm.current().getExecutable("java").absolutePath

        environment["HALSIM_EXTENSIONS"] = env
        if (OperatingSystem.current().isUnix()) {
            environment["LD_LIBRARY_PATH"] = ldpath
            environment["DYLD_FALLBACK_LIBRARY_PATH"] = ldpath // On Mac it isn't 'safe' to override the non-fallback version.
        } else if (OperatingSystem.current().isWindows()) {
            environment["PATH"] = ldpath + ";" + System.getenv("PATH")
        }
        persist = true  // So if we crash instantly you can still see the output
        headless = true
        launch(java, "-Djava.library.path=${ldpath}", "-jar", jar.archivePath.toString())
        // TODO: Add some kind of subsystem here so we can launch externally. It should watch for a stopped build or something
    }
}
