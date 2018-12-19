package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project

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
    }

}
