package jaci.openrio.gradle.sim

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
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
class JavaSimulationTask extends DefaultTask {

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

        def deploymentId = getPath()
        def deploymentRegistry = getDeploymentRegistry()
        def deploymentHandle = deploymentRegistry.get(deploymentId, SimulationDeploymentHandle)

        if (deploymentHandle == null) {
            deploymentHandle = deploymentRegistry.start(
                    deploymentId,
                    DeploymentRegistry.ChangeBehavior.BLOCK,
                    SimulationDeploymentHandle,
                    jar, ldpath, env
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

        private final Jar jar
        private final String ldpath, env
        private final ProcessBuilder builder
        private Process process

        @Inject
        public SimulationDeploymentHandle(Jar jar, String ldpath, String env) {
            this.jar = jar
            this.ldpath = ldpath
            this.env = env
            builder = new ProcessBuilder(
                    Jvm.current().getExecutable("java").absolutePath,
                    "-Djava.library.path=${ldpath}",
                    "-jar", jar.archivePath.toString()
            )
            builder.environment().put("HALSIM_EXTENSIONS", env)
            if (OperatingSystem.current().isUnix()) {
                builder.environment().put("LD_LIBRARY_PATH", ldpath)
                builder.environment().put("DYLD_FALLBACK_LIBRARY_PATH", ldpath)  // On Mac it isn't 'safe' to override the non-fallback version.
            } else if (OperatingSystem.current().isWindows()) {
                builder.environment().put("PATH", ldpath + ";" + System.getenv("PATH"))
            }
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
