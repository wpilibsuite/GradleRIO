package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.toolchain.NativePlatforms
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
        def common = { NativeLib lib ->
            lib.headerDirs = []
            lib.sourceDirs = []
            lib.staticMatchers = []
            lib.debugMatchers = ['**/*.pdb', '**/*.so.debug']
            lib.sharedMatchers = []
            lib.dynamicMatchers = []
        }

        wpi.deps.vendor.dependencies.each { WPIVendorDepsExtension.JsonDependency dep ->
            dep.cppDependencies.each { WPIVendorDepsExtension.CppArtifact cpp ->

                String linkSuff     = cpp.sharedLibrary ? '' : 'static'
                String name = dep.uuid + cpp.libName
                String mavenbase = "${cpp.groupId}:${cpp.artifactId}:${WPIVendorDepsExtension.getVersion(cpp.version, wpi)}"
                String config = cpp.configuration ?: "native_${dep.uuid}_${cpp.groupId}${cpp.artifactId}"
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
                        lib.libraryName = "${name}_headers"
                        lib.maven = "$mavenbase:${cpp.headerClassifier}@zip"
                        lib.configuration = "${config}_headers".toString()
                    } as Action<NativeLib>)
                }

                if (cpp.sourcesClassifier != null) {
                    libs.create("${name}_sources".toString(), NativeLib, { NativeLib lib ->
                        common(lib)
                        // Sources apply to all platforms, even if the binaries are missing.
                        lib.targetPlatforms = allPlatforms
                        lib.sourceDirs << ''
                        lib.libraryName = "${name}_sources"
                        lib.maven = "$mavenbase:${cpp.sourcesClassifier}@zip"
                        lib.configuration = "${config}_sources".toString()
                    } as Action<NativeLib>)
                }


                ['debug', ''].each { String buildKind ->
                    String buildType    = buildKind.contains('debug') ? 'debug' : 'release'
                    String binaryConfig = config + buildKind

                    if (cpp.binaryPlatforms != null) {
                        if (cpp.binaryPlatforms.contains(NativePlatforms.roborio)) {
                            def platform = 'linuxathena'
                            libs.create("${name}_${platform}${buildKind}".toString(), NativeLib, { NativeLib lib ->
                                common(lib)
                                lib.targetPlatforms = [platform]
                                lib.libraryName = "${name}_binaries"

                                lib.buildType = buildType

                                if (cpp.sharedLibrary) {
                                    lib.sharedMatchers = ["**/*${cpp.libName}*.so".toString()]
                                    lib.dynamicMatchers = lib.sharedMatchers
                                } else {
                                    lib.staticMatchers.add("**/*${cpp.libName}*.a".toString())
                                }
                                lib.maven = "$mavenbase:${platform}${linkSuff}$buildKind@zip"
                                // It can't be 'config' otherwise missing libs break even if not used!
                                lib.configuration = "${binaryConfig}_${platform}".toString()
                            } as Action<NativeLib>)
                        }

                        for (String p : cpp.binaryPlatforms) {
                            // Skip athena, as it is specially handled
                            if (p.contains(NativePlatforms.roborio)) {
                                continue
                            }
                            // DON'T REMOVE THIS!
                            // I know it's a redundant variable, but it's actually required. Groovy sucks with variable
                            // lifetime, so if you remove this, platform as read inside of the action for libs.create
                            // will only equal the last registered platform. The action is delegated and the variable is
                            // overridden before the action is called, but groovy is too dumb to realize that itself.
                            String platform = p
                            libs.create("${name}_${platform}${buildKind}".toString(), NativeLib, { NativeLib lib ->
                                common(lib)
                                lib.targetPlatforms = [platform]
                                lib.libraryName = "${name}_binaries"

                                lib.buildType = buildType

                                lib.staticMatchers = ["**/*${cpp.libName}*.lib".toString()]
                                if (cpp.sharedLibrary) {
                                    lib.sharedMatchers = ["**/*${cpp.libName}*.so".toString(), "**/*${cpp.libName}*.dylib".toString()]

                                    lib.dynamicMatchers = lib.sharedMatchers + "**/${cpp.libName}*.dll".toString()
                                } else {
                                    lib.staticMatchers.add("**/*${cpp.libName}*.a".toString())
                                }
                                lib.maven = "$mavenbase:$platform$linkSuff$buildKind@zip"
                                // It can't be 'config' otherwise missing libs break even if not used!
                                lib.configuration = "${binaryConfig}_${platform}".toString()
                            } as Action<NativeLib>)
                        }
                    }
                }
            }
        }
    }
}
