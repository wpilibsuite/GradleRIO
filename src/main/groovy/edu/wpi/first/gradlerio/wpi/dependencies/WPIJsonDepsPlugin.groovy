package edu.wpi.first.gradlerio.wpi.dependencies


import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.DelegatedDependencySet
import jaci.gradle.nativedeps.DependencySpecExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.language.nativeplatform.DependentSourceSet
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
        boolean skipOnUnknownClassifier
        String[] validClassifiers
    }

    static class CppArtifact {
        String groupId
        String artifactId
        String version
        boolean isHeaderOnly
        String headerClassifier
        boolean hasSources
        String sourcesClassifier
        boolean sharedLibrary
        String libName
        boolean skipOnUnknownClassifier
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

    @CompileStatic
    static class WPIJsonDepsExtension {
        List<JsonDependency> dependencies = []
        final List<DelegatedDependencySet> nativeDependenciesList = []

        final Project project

        WPIJsonDepsExtension(Project project) {
            this.project = project
        }
    }

    @CompileStatic
    static class MissingJniDependencyException extends RuntimeException {
        String dependencyName
        String classifier
        JniArtifact artifact

        MissingJniDependencyException(String name, String classifier, JniArtifact artifact) {
            super("Cannot find jni dependency: ${name} for classifier: ${classifier}".toString())
            this.dependencyName = name
            this.classifier = classifier
            this.artifact = artifact
        }
    }

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

        def jsonExtension = project.extensions.create('wpiJsonDeps', WPIJsonDepsExtension, project)

        def nativeclassifier = wpi.nativeClassifier

        def jsonDepFolder = project.file('vendordeps')
        JsonSlurper slurper = new JsonSlurper()

        // Try to load dependencies JSON files
        if (jsonDepFolder.exists()) {
            jsonDepFolder.eachFileMatch FileType.FILES,~/.*\.json/, { File file ->
                file.withReader {
                    def slurped = slurper.parse(it)
                    def dep = constructJsonDependency(slurped)
                    if (dep == null) {
                        //TODO Display an error
                        println "Error loading Vendor File ${file.toString()}"
                    } else {
                        jsonExtension.dependencies << dep
                    }
                }
            }
        }

        // Add all URLs from dependencies
        jsonExtension.dependencies.each { JsonDependency dep ->
            dep.mavenUrls.each { url ->
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.url = url
                }
            }
        }
        project.extensions.add('javaVendorLibraries', { String... ignoreLibraries ->
            if (jsonExtension.dependencies != null) {
                return jsonExtension.dependencies.findAll { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.collectMany { JsonDependency dep ->
                    dep.javaDependencies.collect { JavaArtifact art -> "${art.groupId}:${art.artifactId}:${art.version}" } as Collection
                }
            } else {
                return []
            }
        })

        project.extensions.add('jniRoboRIOVendorLibraries', { String... ignoreLibraries ->
            def classifier = 'linuxathena'
            if (jsonExtension.dependencies != null) {
                return jsonExtension.dependencies.findAll { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.collectMany { JsonDependency dep ->
                    dep.jniDependencies.find { JniArtifact art ->
                        def containsClassifier = art.validClassifiers.contains(classifier)
                        if (!containsClassifier && !art.skipOnUnknownClassifier) {
                            throw new MissingJniDependencyException(dep.name, classifier, art)
                        }
                        return containsClassifier
                    }.collect { JniArtifact art ->
                        "${art.groupId}:${art.artifactId}:${art.version}:${classifier}@${art.isJar ? 'jar' : 'zip'}"
                    } as Collection
                }
            } else {
                return []
            }
        })

        project.extensions.add('jniClassifierVendorLibraries', { String classifier, String... ignoreLibraries ->
            if (jsonExtension.dependencies != null) {
                return jsonExtension.dependencies.findAll { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.collectMany { JsonDependency dep ->
                    dep.jniDependencies.find { JniArtifact art ->
                        def containsClassifier = art.validClassifiers.contains(classifier)
                        if (!containsClassifier && !art.skipOnUnknownClassifier) {
                            throw new MissingJniDependencyException(dep.name, classifier, art)
                        }
                        return containsClassifier
                    }.collect { JniArtifact art ->
                        "${art.groupId}:${art.artifactId}:${art.version}:${classifier}@${art.isJar ? 'jar' : 'zip'}"
                    } as Collection
                }
            } else {
                return []
            }
        })

        project.extensions.add('jniDesktopVendorLibraries', { String... ignoreLibraries ->
            def classifier = nativeclassifier
            if (jsonExtension.dependencies != null) {
                return jsonExtension.dependencies.findAll { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.collectMany { JsonDependency dep ->
                    dep.jniDependencies.find { JniArtifact art ->
                        def containsClassifier = art.validClassifiers.contains(classifier)
                        if (!containsClassifier && !art.skipOnUnknownClassifier) {
                            throw new MissingJniDependencyException(dep.name, classifier, art)
                        }
                        return containsClassifier
                    }.collect { JniArtifact art ->
                        "${art.groupId}:${art.artifactId}:${art.version}:${classifier}@${art.isJar ? 'jar' : 'zip'}"
                    } as Collection
                }
            } else {
                return []
            }
        })

        project.extensions.add('useCppVendorLibraries', { Object closureArg, String... ignoreLibraries ->
            useCppVendorLibraries(project, jsonExtension, closureArg, ignoreLibraries)
        })
    }

    private void useCppVendorLibraries(Project project, WPIJsonDepsExtension jsonExtension, Object closureArg, String... ignoreLibraries) {
        def dse = project.extensions.getByType(DependencySpecExtension)
        if (closureArg in TargetedNativeComponent) {
            TargetedNativeComponent component = (TargetedNativeComponent)closureArg
            component.binaries.withType(NativeBinarySpec).all { NativeBinarySpec bin ->
                Set<DelegatedDependencySet> dds = []
                jsonExtension.dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.each { JsonDependency dep ->
                    dep.cppDependencies.collect { CppArtifact art ->
                        dds << new DelegatedDependencySet(dep.uuid + art.libName, bin, dse, art.skipOnUnknownClassifier)
                    }
                }

                bin.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                    dds.each { DelegatedDependencySet set ->
                        dss.lib(set)
                    }
                }

            }
        } else if (closureArg in NativeBinarySpec) {
            NativeBinarySpec bin = (NativeBinarySpec) closureArg
            Set<DelegatedDependencySet> dds = []
            jsonExtension.dependencies.find { (!ignoreLibraries.contains(it.name) && !ignoreLibraries.contains(it.uuid)) }.each { JsonDependency dep ->
                dep.cppDependencies.collect { CppArtifact art ->
                    dds << new DelegatedDependencySet(dep.uuid + art.libName, bin, dse, art.skipOnUnknownClassifier)
                }
            }

            bin.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                dds.each { DelegatedDependencySet set ->
                    dss.lib(set)
                }
            }
        } else {
            throw new GradleException('Unknown type for useVendorLibraries target. You put this declaration in a weird place.')
        }
    }
}
