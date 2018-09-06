package edu.wpi.first.gradlerio.wpi.dependencies

import com.google.gson.Gson
import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.TargetedNativeComponent

@CompileStatic
class WPIJsonDepsPlugin implements Plugin<Project> {

    static class Artifact {
        String groupId
        String artifactId
        String version
        String[] validClassifiers
    }

    static class JsonDependency {
        String name
        String uuid
        Artifact[] javaDependencies
        Artifact[] jniDependencies
        Artifact[] cppDependencies
    }

    JsonDependency[] dependencies

    @Override
    void apply(Project project) {
        def wpi = project.extensions.getByType(WPIExtension)

        // Try to load dependencies JSON file
        File jsonDepFile = project.file('dependencies.json')
        if (jsonDepFile.exists()) {

            jsonDepFile.withReader {
                dependencies = new Gson().fromJson(it, JsonDependency[])
            }
        }

        project.configurations.maybeCreate('wpiJsonJni')

        project.extensions.add('javaVendorLibraries', { String... ignoreLibraries ->
            def returns = []
            if (dependencies != null) {
                dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) } each { JsonDependency dep ->
                    dep.javaDependencies.each { Artifact art ->
                        returns << "${art.groupId}:${art.artifactId}:${art.version}"
                    }
                    dep.jniDependencies.each { Artifact art ->
                        art.validClassifiers.each {
                            project.dependencies.add('wpiJsonJni', "${art.groupId}:${art.artifactId}:${art.version}:${it}@zip")
                        }
                    }
                }
            }
            return returns
        })

        project.extensions.add('useCppVendorLibraries', { Object closureArg, String... ignoreLibraries ->
            if (closureArg in TargetedNativeComponent) {

            } else if (closureArg in NativeBinarySpec) {

            } else {
                throw new GradleException('Unknown type for useVendorLibraries target. You put this declaration in a weird place.')
            }
        })
    }
}
