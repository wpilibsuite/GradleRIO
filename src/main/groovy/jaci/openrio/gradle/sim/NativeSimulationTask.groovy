package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable

import javax.inject.Inject

@CompileStatic
class NativeSimulationTask extends DefaultTask {

    NativeExecutableBinarySpec binary

    @TaskAction
    void run() {
        // TODO: Spawn in new window (gradle daemon keeps this process alive)
        // OR: Write a script that does this for the user?
        //          i.e. task outputs a runnable file that the user then uses.
        def installTask = binary.tasks.withType(InstallExecutable).first()
        def env = SimulationPlugin.getHALExtensionsEnvVar(project)
        println "Using Environment: HALSIM_EXTENSIONS=${env}"
//        project.exec { ExecSpec spec ->
//            spec.environment.put("HALSIM_EXTENSIONS", env)
//            spec.commandLine(installTask.runScript)
//
//        }

        def deploymentId = getPath()
        def deploymentRegistry = getDeploymentRegistry()
        def deploymentHandle = deploymentRegistry.get(deploymentId, SimulationDeploymentHandle)

        if (deploymentHandle == null) {
            deploymentHandle = deploymentRegistry.start(
                    deploymentId,
                    DeploymentRegistry.ChangeBehavior.BLOCK,
                    SimulationDeploymentHandle,
                    installTask, env
            )
        }
    }

    // NOTE: This is all internal stuff. As gradle gets updates, this is subject to a LOT of change
    // Usually we would use javaexec, but these tasks don't end, they just keep running.
    // For that reason, we're using the Deployment API, which is internal.
    // TL;DR, the Deployment API is used when executing tasks that are long running (continuous) to
    // help them work with the Daemon
    // https://github.com/gradle/gradle/issues/2336
    @Inject
    public DeploymentRegistry getDeploymentRegistry() {
        throw new UnsupportedOperationException()
    }

    @CompileStatic
    public static class SimulationDeploymentHandle implements DeploymentHandle {

        private final InstallExecutable installTask
        private final String env
        private final ProcessBuilder builder
        private Process process

        @Inject
        public SimulationDeploymentHandle(InstallExecutable installTask, String env) {
            this.installTask = installTask
            this.env = env
            builder = new ProcessBuilder()
            builder.environment().put("HALSIM_EXTENSIONS", env)
            def dir = new File(installTask.installDirectory.asFile.get(), "lib")
            builder.directory(installTask.installDirectory.asFile.get())
            builder.command(new File(dir, installTask.sourceFile.asFile.get().name).absolutePath)

            if (OperatingSystem.current().isUnix()) {
                builder.environment().put("LD_LIBRARY_PATH", dir.absolutePath)
                builder.environment().put("DYLD_FALLBACK_LIBRARY_PATH", dir.absolutePath)  // On Mac it isn't 'safe' to override the non-fallback version.
            }

            println builder.command()
        }

        @Override
        boolean isRunning() {
            return process != null && process.isAlive()
        }

        @Override
        void start(Deployment deployment) {
            process = builder.start()
            process.consumeProcessOutputStream(System.out as OutputStream)
            process.consumeProcessErrorStream(System.err as OutputStream)
        }

        @Override
        void stop() {
            process.destroyForcibly()
        }
    }

}
