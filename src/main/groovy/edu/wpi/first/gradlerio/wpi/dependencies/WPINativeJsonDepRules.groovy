package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

@CompileStatic
class WPINativeJsonDepRules extends RuleSource {
        @Mutate
        void addJsonLibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def jsonExtension = extensionContainer.getByType(WPIJsonDepsPlugin.WPIJsonDepsExtension)
            def common = { NativeLib lib ->
                lib.targetPlatforms = [wpi.platforms.roborio]
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.staticMatchers = []
                lib.sharedMatchers = []
                lib.dynamicMatchers = []
            }

//            def nativeclassifier = wpi.nativeClassifier
//
//            jsonExtension.dependencies.each { WPIJsonDepsPlugin.JsonDependency dep ->
//                dep.cppDependencies.each { WPIJsonDepsPlugin.CppArtifact art ->
//                    def name = dep.uuid + art.libName
//                    def supportNative = art.validClassifiers.contains(nativeclassifier)
//                    def supportAthena = art.validClassifiers.contains('linuxathena')
//                    def mavenBase = "${art.groupId}:${art.artifactId}:${art.version}"
//                    def cfgName = art.configuration ?: "native_${dep.uuid}"
//
//                    if (art.headerClassifier != null) {
//                        libs.create("${name}_headers", NativeLib) { NativeLib lib ->
//                            common(lib)
//                            if (supportNative)
//                                lib.targetPlatforms << wpi.platforms.desktop
//                            lib.headerDirs << ''
//                            lib.maven = "${mavenBase}:${art.headerClassifier}@zip"
//                            lib.configuration = cfgName
//                            null
//                        }
//                    }
//
//                    if (art.isHeaderOnly) {
//                        // Create header only lib
//                        libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
//                            lib.libs << "${name}_headers".toString()
//                            lib.targetPlatforms = [wpi.platforms.roborio]
//                            if (supportNative)
//                                lib.targetPlatforms << wpi.platforms.desktop
//                            null
//                        }
//                        return
//                    }
//
//                    if (supportAthena) {
//                        libs.create("${name}_athena", NativeLib) { NativeLib lib ->
//                            common(lib)
//                            lib.libraryName = "${name}_binaries"
//                            if (art.sharedLibrary) {
//                                lib.sharedMatchers = ["**/lib${art.libName}.so".toString()]
//                                lib.dynamicMatchers = lib.sharedMatchers
//                            } else {
//                                lib.staticMatchers = ["**/static/*.a".toString()]
//                            }
//                            lib.maven = "${mavenBase}:linuxathena@zip"
//                            lib.configuration = cfgName
//                            null
//                        }
//                    }
//
//                    if (art.hasSources) {
//                        libs.create("${name}_sources", NativeLib) { NativeLib lib ->
//                            common(lib)
//                            if (supportNative)
//                                lib.targetPlatforms << wpi.platforms.desktop
//                            lib.sourceDirs << ''
//                            lib.maven = "${mavenBase}:${art.sourcesClassifier}@zip"
//                            lib.configuration = cfgName
//                            null
//                        }
//                    }
//
//                    if (supportNative && nativeclassifier != null) {
//                        libs.create("${name}_native", NativeLib) { NativeLib lib ->
//                            common(lib)
//                            lib.libraryName = "${name}_binaries"
//                            lib.targetPlatforms = [wpi.platforms.desktop]
//                            if (art.sharedLibrary) {
//                                lib.staticMatchers = ["**/*${art.libName}.lib".toString()]
//                                lib.sharedMatchers = ["**/*${art.libName}.so".toString(), "**/*${art.libName}.dylib".toString()]
//
//                                lib.dynamicMatchers = lib.sharedMatchers + "**/${art.libName}.dll".toString()
//                            } else {
//                                lib.staticMatchers = ["**/*${art.libName}.lib".toString(), "**/*${art.libName}.so".toString()]
//                            }
//                            lib.maven = "${mavenBase}:${nativeclassifier}@zip"
//                            lib.configuration = "${cfgName}_desktop"
//                            null
//                        }
//                    }
//
//                    libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
//                        lib.libs << "${name}_binaries".toString() << "${name}_headers".toString()
//                        if (art.hasSources) {
//                            lib.libs << "${name}_sources".toString()
//                        }
//                        lib.targetPlatforms = [wpi.platforms.roborio]
//                        if (supportNative)
//                            lib.targetPlatforms << wpi.platforms.desktop
//                        null
//                    }
//                }
//            }
        }
    }
