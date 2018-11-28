package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.WPIMavenRepo
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class WPIJsonDepsPlugin implements Plugin<Project> {

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
        def wpi = project.extensions.getByType(WPIExtension)
        def vendorExt = project.extensions.create('frcVendor', WPIVendorDepsExtension, project)

        def logger = ETLoggerFactory.INSTANCE.create("WPIJsonDeps")

        JsonSlurper slurper = new JsonSlurper()

        // Try to load dependencies JSON files
        vendorExt.allVendorFiles().each { File f ->
            f.withReader {
                def slurped = slurper.parse(it)
                try {
                    vendorExt.loadDependency(slurped)
                } catch (e) {
                    logger.logError("Malformed Vendor Deps File: ${f.toString()}")
                }
            }
        }

        // Add all URLs from dependencies
        vendorExt.dependencies.each { WPIVendorDepsExtension.JsonDependency dep ->
            int i = 0
            dep.mavenUrls.each { url ->
                wpi.maven.vendor("${dep.uuid}_${i++}") { WPIMavenRepo repo ->
                    repo.release = url
                }
            }
        }
    }
}
