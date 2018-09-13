package edu.wpi.first.gradlerio.wpi.dependencies

import com.google.gson.Gson
import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.TargetedNativeComponent

@CompileStatic
class WPIJsonDepsPlugin implements Plugin<Project> {

    static class JavaArtifact {
        String groupId
        String artifactId
        String version
    }

    static class JniArtifact {
        String groupId
        String artifactId
        String version
        boolean isJar
        String[] validClassifiers
    }

    static class CppArtifact {
        String groupId
        String artifactId
        String version
        boolean isHeaderOnly
        String headerClassifier
        String[] validClassifiers
    }

    static class JsonDependency {
        String name
        String version
        String uuid
        String[] mavenUrls
        String jsonUrl
        String fileName
        JavaArtifact[] javaDependencies
        JniArtifact[] jniDependencies
        CppArtifact[] cppDependencies
    }

    List<JsonDependency> dependencies = []

    @CompileDynamic
    private JsonDependency constructJsonDependency(Object slurped) {
        try {
            return new JsonDependency(slurped)
        } catch (def e) {
            return null
        }
    }

    @Override
    void apply(Project project) {
        def wpi = project.extensions.getByType(WPIExtension)

        def nativeclassifier = (
                OperatingSystem.current().isWindows() ?
                        System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                        OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                        null
        )

        def jsonDepFolder = project.file('vendordeps')
        JsonSlurper slurper = new JsonSlurper()

        // Try to load dependencies JSON files
        if (jsonDepFolder.exists()) {
            jsonDepFolder.eachFileMatch FileType.FILES,~/.*\.json/, { File file ->
                println file
                file.withReader {
                    def slurped = slurper.parse(it)
                    def dep = constructJsonDependency(slurped)
                    if (dep == null) {
                        //TODO Display an error
                    } else {
                        dependencies << dep
                    }
                }
            }
        }

        // Add all URLs from dependencies
        dependencies.each { JsonDependency dep ->
            dep.mavenUrls.each { url ->
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.url = url
                }
            }
        }

        project.configurations.maybeCreate('wpiJsonJni')

        project.extensions.add('javaVendorLibraries', { String... ignoreLibraries ->
            def returns = []
            if (dependencies != null) {
                dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) } each { JsonDependency dep ->
                    dep.javaDependencies.each { JavaArtifact art ->
                        returns << "${art.groupId}:${art.artifactId}:${art.version}"
                    }
                }
            }
            return returns
        })

        project.extensions.add('jniRoboRIOVendorLibraries', { String... ignoreLibraries ->
            def returns = []
            if (dependencies != null) {
                dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) } each { JsonDependency dep ->
                    dep.jniDependencies.find{it.validClassifiers.contains('linuxathena')}.each { JniArtifact art ->
                        project.dependencies.add('nativeZip', "${art.groupId}:${art.artifactId}:${art.version}:${'linuxathena'}@${art.isJar ? 'jar' : 'zip'}")
                    }
                }
            }
            return returns
        })

        project.extensions.add('jniClassifierVendorLibraries', { String classifier, String... ignoreLibraries ->
            def returns = []
            if (dependencies != null) {
                dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) } each { JsonDependency dep ->
                    dep.jniDependencies.find{it.validClassifiers.contains(classifier)}.each { JniArtifact art ->
                        project.dependencies.add('nativeZip', "${art.groupId}:${art.artifactId}:${art.version}:${classifier}@${art.isJar ? 'jar' : 'zip'}")
                    }
                }
            }
            return returns
        })

        project.extensions.add('jniDesktopVendorLibraries', { String... ignoreLibraries ->
            def returns = []
            if (dependencies != null) {
                dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) } each { JsonDependency dep ->
                    dep.jniDependencies.find{it.validClassifiers.contains(nativeclassifier)}.each { JniArtifact art ->
                        project.dependencies.add('nativeDesktopZip', "${art.groupId}:${art.artifactId}:${art.version}:${nativeclassifier}@${art.isJar ? 'jar' : 'zip'}")
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
