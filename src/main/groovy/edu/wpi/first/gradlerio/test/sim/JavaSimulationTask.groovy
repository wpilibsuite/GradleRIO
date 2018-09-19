package edu.wpi.first.gradlerio.test.sim

import edu.wpi.first.gradlerio.ExternalLaunchTask
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaSimulationTask extends ExternalLaunchTask {
    @Internal
    Jar jar

    @TaskAction
    void run() {
        def env = SimulationPlugin.getHALExtensionsEnvVar(project)
        println "Using Environment: HALSIM_EXTENSIONS=${env}"
        def ldpath = TestPlugin.jniExtractionDir(project).absolutePath

        def java = OperatingSystem.current().isWindows() ? "java" : Jvm.current().getExecutable("java").absolutePath

        environment["HALSIM_EXTENSIONS"] = env
        if (OperatingSystem.current().isUnix()) {
            environment["LD_LIBRARY_PATH"] = ldpath
            environment["DYLD_FALLBACK_LIBRARY_PATH"] = ldpath // On Mac it isn't 'safe' to override the non-fallback version.
        } else if (OperatingSystem.current().isWindows()) {
            environment["PATH"] = ldpath + ";" + System.getenv("PATH")
        }
        persist = true  // So if we crash instantly you can still see the output
        scriptOnly = true
        launch(java, "-Djava.library.path=${ldpath}", "-jar", jar.archivePath.toString())
        // TODO: Add some kind of subsystem here so we can launch externally. It should watch for a stopped build or something
    }
}
