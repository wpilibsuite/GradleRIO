package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.frc.FRCJREArtifact
import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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

        // We need to register our own task for this, since .doFirst on compileJava (or any Jar task), won't work
        // if it's up-to-date
        def lazyPreempt = project.tasks.register('downloadDepsPreemptively', DefaultTask, { Task t ->
            t.doFirst {
                // On build, download all libs that will be needed for deploy to lessen the cases where the user has to
                // run an online deploy dry or downloadAll task.
                downloadDepsPreemptively(project, logger)
            }
        } as Action<Task>)

        project.tasks.register("vendordep", VendorDepTask) { VendorDepTask task ->
            task.group = "GradleRIO"
            task.description = "Install vendordep JSON file from URL or local wpilib folder"
        }

        project.tasks.withType(Jar) { Jar jarTask ->
            jarTask.dependsOn(lazyPreempt)
        }
    }

    void downloadDepsPreemptively(Project project, ETLogger logger) {
        def configs = ['nativeLib', 'nativeZip']

        project.extensions.getByType(DeployExtension).artifacts.each { Artifact art ->
            if (art instanceof FRCJREArtifact) {
                def cfgName = ((FRCJREArtifact)art).configuration()
                logger.info("Found JRE Configuration: " + cfgName)
                configs.add(cfgName);
            }
        }

        configs.each {
            def cfg = project.configurations.getByName(it)
            if (cfg.canBeResolved) {
                logger.info("Resolving Deps Configuration: " + cfg.getName())
                cfg.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact art ->
                    logger.info(' ->' + art.file)
                }
            } else {
                logger.info("Can't resolve: " + cfg.getName())
            }
        }
    }

}
