package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.gradle.process.JavaExecSpec

@CompileStatic
class JavaSimulationTask extends DefaultTask {

    Jar jar

    @TaskAction
    void run() {
        // Extract necessary libs TODO make this a new task so it can be cached
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

        // TODO: Spawn in new window (gradle daemon keeps this process alive)
        // OR: Write a script that does this for the user?
        //          i.e. task outputs a runnable file that the user then uses.
        def env = SimulationPlugin.getHALExtensionsEnvVar(project)
        println "Using Environment: HALSIM_EXTENSIONS=${env}"
        def ldpath = libdirs.join(SimulationPlugin.envDelimiter())
        project.javaexec { JavaExecSpec spec ->
            spec.environment.put("HALSIM_EXTENSIONS", env)

            if (OperatingSystem.current().isUnix()) {
                spec.environment.put("LD_LIBRARY_PATH", ldpath)
                spec.environment.put("DYLD_FALLBACK_LIBRARY_PATH", ldpath)  // On Mac it isn't 'safe' to override the non-fallback version.
            } else if (OperatingSystem.current().isWindows()) {
                spec.environment.put("PATH", ldpath + ";" + System.getenv("PATH"))
            }

            spec.jvmArgs("-Djava.library.path=${ldpath}")
            spec.setClasspath(project.configurations.getByName('compile'))
            spec.setMain("-jar")
            spec.args(jar.archivePath)
        }
    }

}
