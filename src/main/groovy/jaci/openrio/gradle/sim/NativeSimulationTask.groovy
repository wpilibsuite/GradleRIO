package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import jaci.openrio.gradle.ExternalLaunchTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable

import javax.inject.Inject

@CompileStatic
class NativeSimulationTask extends ExternalLaunchTask {

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
        workingDir = installTask.installDirectory.asFile.get()
        persist = true
        scriptOnly = true
        launch(installTask.sourceFile.asFile.get().absolutePath)
        // TODO: Add some kind of subsystem here so we can launch externally. It should watch for a stopped build or something
    }

}
