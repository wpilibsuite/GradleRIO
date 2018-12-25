package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.jvm.tasks.Jar

@CompileStatic
class WPIDependenciesPlugin implements Plugin<Project> {

    @CompileStatic
    static class MissingJniDependencyException extends RuntimeException {
        String dependencyName
        String classifier
        WPIVendorDepsExtension.JniArtifact artifact

        MissingJniDependencyException(String name, String classifier, WPIVendorDepsExtension.JniArtifact artifact) {
            super("Cannot find jni dependency: ${name} for classifier: ${classifier}".toString())
            this.dependencyName = name
            this.classifier = classifier
            this.artifact = artifact
        }
    }

    @Override
    void apply(Project project) {
        def logger = ETLoggerFactory.INSTANCE.create("WPIDeps")
        def wpi = project.extensions.getByType(WPIExtension)
        wpi.deps.vendor.loadAll()

        project.tasks.withType(Jar) { Jar jarTask ->
            jarTask.doFirst {
                // On build, download all libs that will be needed for deploy to lessen the cases where the user has to
                // run an online deploy dry or downloadAll task.
                downloadRoborioDepsForDeploy(project, logger)
            }
        }
    }

    void downloadRoborioDepsForDeploy(Project project, ETLogger logger) {
        ['nativeLib', 'nativeZip'].each {
            def cfg = project.configurations.getByName(it)
            if (cfg.canBeResolved) {
                logger.info("Resolving RoboRIO Deps Configuration: " + cfg.getName())
                cfg.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact art ->
                    art.file
                }
            } else {
                logger.info("Can't resolve: " + cfg.getName())
            }
        }
    }

}
