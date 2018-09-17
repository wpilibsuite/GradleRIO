package edu.wpi.first.gradlerio.wpi.dependencies

import com.google.gson.Gson
import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.NativeDepsPlugin
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.DelegatedDependencySet
import jaci.gradle.nativedeps.DependencySpecExtension
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.nativeplatform.DependentSourceSet
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
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
        project.pluginManager.apply(NativeDepsPlugin)
        def wpi = project.extensions.getByType(WPIExtension)

        def jsonExtension = project.extensions.create('wpiJsonDeps', WPIJsonDepsExtension, project)

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

        DependencySpecExtension dse = project.extensions.getByType(DependencySpecExtension)

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
        })
    }

    static class WPIJsonDepRules extends RuleSource {
        @Mutate
        void addJsonLibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def jsonExtension = extensionContainer.getByType(WPIJsonDepsExtension)
            def common = { NativeLib lib ->
                lib.targetPlatforms = ['roborio']
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.staticMatchers = []
                lib.sharedMatchers = []
                lib.dynamicMatchers = []
            }

            def nativeclassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                            null
            )

            jsonExtension.dependencies.each { JsonDependency dep ->
                dep.cppDependencies.each { CppArtifact art ->
                    def name = dep.uuid + art.libName
                    def supportNative = art.validClassifiers.contains(nativeclassifier)
                    def supportAthena = art.validClassifiers.contains('linuxathena')
                    def mavenBase = "${art.groupId}:${art.artifactId}:${art.version}"
                    def cfgName = "native_${name}"
                    libs.create("${name}_headers", NativeLib) { NativeLib lib ->
                        common(lib)
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        lib.headerDirs << ''
                        lib.maven = "${mavenBase}:${art.headerClassifier}@zip"
                        lib.configuration = cfgName
                        null
                    }

                    if (art.isHeaderOnly) {
                        // Create header only lib
                        libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
                            lib.libs << "${name}_headers".toString()
                            lib.targetPlatforms = ['roborio']
                            if (supportNative)
                                lib.targetPlatforms << 'desktop'
                            null
                        }
                        return
                    }

                    if (supportAthena) {
                        libs.create("${name}_athena", NativeLib) { NativeLib lib ->
                            common(lib)
                            lib.libraryName = "${name}_binaries"
                            if (art.sharedLibrary) {
                                lib.sharedMatchers = ["**/lib${art.libName}.so".toString()]
                                lib.dynamicMatchers = lib.sharedMatchers
                            } else {
                                lib.staticMatchers = ["**/static/*.a".toString()]
                            }
                            lib.maven = "${mavenBase}:linuxathena@zip"
                            lib.configuration = cfgName
                            null
                        }
                    }

                    if (art.hasSources) {
                        libs.create("${name}_sources", NativeLib) { NativeLib lib ->
                            common(lib)
                            if (supportNative)
                                lib.targetPlatforms << 'desktop'
                            lib.sourceDirs << ''
                            lib.maven = "${mavenBase}:${art.sourcesClassifier}@zip"
                            lib.configuration = cfgName
                            null
                        }
                    }

                    if (supportNative && nativeclassifier != null) {
                        libs.create("${name}_native", NativeLib) { NativeLib lib ->
                            common(lib)
                            lib.libraryName = "${name}_binaries"
                            lib.targetPlatforms = ['desktop']
                            if (art.sharedLibrary) {
                                lib.staticMatchers = ["**/*${art.libName}.lib".toString()]
                                lib.sharedMatchers = ["**/*${art.libName}.so".toString(), "**/*${art.libName}.dylib".toString()]

                                lib.dynamicMatchers = lib.sharedMatchers + "**/${art.libName}.dll".toString()
                            } else {
                                lib.staticMatchers = ["**/*${art.libName}.lib".toString(), "**/*${art.libName}.so".toString()]
                            }
                            lib.maven = "${mavenBase}:${nativeclassifier}@zip"
                            lib.configuration = "${cfgName}_desktop"
                            null
                        }
                    }

                    libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
                        lib.libs << "${name}_binaries".toString() << "${name}_headers".toString()
                        if (art.hasSources) {
                            lib.libs << "${name}_sources".toString()
                        }
                        lib.targetPlatforms = ['roborio']
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        null
                    }
                }
            }
        }
    }
}
