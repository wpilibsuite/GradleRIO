package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.platform.base.Platform
import org.gradle.platform.base.PlatformContainer

@CompileStatic
class WPINativeJsonDepRules extends RuleSource {
        @Mutate
        void addJsonLibraries(NativeDepsSpec libs, final PlatformContainer platformContainer, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def vendorExt = extensionContainer.getByType(WPIVendorDepsExtension)
            def common = { NativeLib lib ->
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.staticMatchers = []
                lib.sharedMatchers = []
                lib.dynamicMatchers = []
            }

            vendorExt.dependencies.each { WPIVendorDepsExtension.JsonDependency dep ->
                dep.cppDependencies.each { WPIVendorDepsExtension.CppArtifact cpp ->
                    String name = dep.uuid + cpp.libName
                    String mavenbase = "${cpp.groupId}:${cpp.artifactId}:${cpp.version}"
                    String config = cpp.configuration ?: "native_${dep.uuid}"
                    List<String> allPlatforms = platformContainer.collect { Platform p -> p.name }

                    // Note: because of a discrepancy between the target platforms of the headers, sources
                    // and the binaries, we don't provide a CombinedNativeLib, meaning all of _binaries,
                    // _headers and _sources must be applied. We do this in WPIVendorDepsExtension#cppVendorLibForBin

                    if (cpp.headerClassifier != null) {
                        libs.create("${name}_headers".toString(), NativeLib, { NativeLib lib ->
                            common(lib)
                            // Headers apply to all platforms, even if the binaries are missing.
                            lib.targetPlatforms = allPlatforms
                            lib.headerDirs << ''
                            lib.maven = "$mavenbase:${cpp.headerClassifier}@zip"
                            lib.configuration = config
                        } as Action<NativeLib>)
                    }

                    if (cpp.sourcesClassifier != null) {
                        libs.create("${name}_sources".toString(), NativeLib, { NativeLib lib ->
                            common(lib)
                            // Sources apply to all platforms, even if the binaries are missing.
                            lib.targetPlatforms = allPlatforms
                            lib.sourceDirs << ''
                            lib.maven = "$mavenbase:${cpp.sourcesClassifier}@zip"
                            lib.configuration = config
                        } as Action<NativeLib>)
                    }

                    if (cpp.binaryPlatforms != null) {
                        for (String platform : cpp.binaryPlatforms) {
                            libs.create("${name}_${platform}".toString(), NativeLib, { NativeLib lib ->
                                common(lib)
                                lib.targetPlatforms = [platform]
                                lib.libraryName = "${name}_binaries"

                                lib.staticMatchers = ["**/*${cpp.libName}.lib".toString()]
                                if (cpp.sharedLibrary) {
                                    lib.sharedMatchers = ["**/*${cpp.libName}.so".toString(), "**/*${cpp.libName}.dylib".toString()]

                                    lib.dynamicMatchers = lib.sharedMatchers + "**/${cpp.libName}.dll".toString()
                                } else {
                                    lib.staticMatchers.add("**/*${cpp.libName}.a".toString())
                                }
                                lib.maven = "$mavenbase:$platform@zip"
                                // It can't be 'config' otherwise missing libs break even if not used!
                                lib.configuration = "${config}_${platform}".toString()
                            } as Action<NativeLib>)
                        }
                    }
                }
            }
        }
    }
