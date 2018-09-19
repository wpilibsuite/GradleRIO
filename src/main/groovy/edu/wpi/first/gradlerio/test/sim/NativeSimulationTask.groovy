package edu.wpi.first.gradlerio.test.sim

import edu.wpi.first.gradlerio.ExternalLaunchTask
import groovy.transform.CompileStatic
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable

@CompileStatic
class NativeSimulationTask extends ExternalLaunchTask {
    @Internal
    NativeExecutableBinarySpec binary

    @TaskAction
    void run() {
        def installTask = binary.tasks.withType(InstallExecutable).first()
        def env = SimulationPlugin.getHALExtensionsEnvVar(project)
        println "Using Environment: HALSIM_EXTENSIONS=${env}"
        def dir = new File(installTask.installDirectory.asFile.get(), "lib")

        environment["HALSIM_EXTENSIONS"] = env
        if (OperatingSystem.current().isUnix()) {
            environment["LD_LIBRARY_PATH"] = dir.absolutePath
            environment["DYLD_FALLBACK_LIBRARY_PATH"] = dir.absolutePath
        }
        workingDir = dir
        persist = true
        scriptOnly = true
//        launch(installTask.sourceFile.asFile.get().absolutePath)  // TODO: Gradle 4.7 breaks this
        // TODO: Add some kind of subsystem here so we can launch externally. It should watch for a stopped build or something
    }

}
