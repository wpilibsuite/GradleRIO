package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

@CompileStatic
class WPINativeDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)
        project.pluginManager.apply(ComponentModelBasePlugin)
    }

    static class WPIDepRules extends RuleSource {

        // Note that throughout here we assign lib.configuration,
        // which specifies which configuration each library artifact belongs
        // in. We group them according to which library they represent, which
        // speeds up dependency resolution. When one dependency in a configuration
        // is downloaded, all are downloaded. Unused configurations are not downloaded

        @Mutate
        void addWPILibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def common = { NativeLib lib ->
                lib.targetPlatforms = ['roborio']
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.staticMatchers = []
            }

            def nativeclassifier = wpi.nativeClassifier

            def createWpiLibrary = { String name, String mavenBase, String libname, boolean supportNative ->
                def cfgName = "native_${name}"
                libs.create("${name}_headers", NativeLib) { NativeLib lib ->
                    common(lib)
                    if (supportNative)
                        lib.targetPlatforms << 'desktop'
                    lib.headerDirs << ''
                    lib.maven = "${mavenBase}:headers@zip"
                    lib.configuration = cfgName
                    null
                }

                libs.create("${name}_athena", NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.libraryName = "${name}_binaries"
                    lib.sharedMatchers = ["**/lib${libname}.so".toString()]
                    lib.dynamicMatchers = lib.sharedMatchers
                    lib.maven = "${mavenBase}:linuxathena@zip"
                    lib.configuration = cfgName
                    null
                }

                libs.create("${name}_sources", NativeLib) { NativeLib lib ->
                    common(lib)
                    if (supportNative)
                        lib.targetPlatforms << 'desktop'
                    lib.sourceDirs << ''
                    lib.maven = "${mavenBase}:sources@zip"
                    lib.configuration = cfgName
                    null
                }

                if (supportNative && nativeclassifier != null) {
                    libs.create("${name}_native", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.libraryName = "${name}_binaries"
                        lib.targetPlatforms = ['desktop']
                        lib.staticMatchers = ["**/*${libname}.lib".toString(), "**/*${libname}.a".toString()]
                        lib.sharedMatchers = ["**/*${libname}.so".toString(), "**/*${libname}.dylib".toString()]

                        lib.dynamicMatchers = lib.sharedMatchers + "**/${libname}.dll".toString()
                        lib.maven = "${mavenBase}:${nativeclassifier}@zip"
                        lib.configuration = "${cfgName}_desktop"
                        null
                    }
                }

                libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
                    lib.libs << "${name}_binaries".toString() << "${name}_headers".toString() << "${name}_sources".toString()
                    lib.targetPlatforms = ['roborio']
                    if (supportNative)
                        lib.targetPlatforms << 'desktop'
                    null
                }
            }

            // WPILIB C
            createWpiLibrary('wpilibc', "edu.wpi.first.wpilibc:wpilibc-cpp:${wpi.wpilibVersion}", 'wpilibc', true)

            // HAL
            createWpiLibrary('hal', "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}", 'wpiHal', true)


            // NI LIBS
            libs.create('ni_chipobject_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.sharedMatchers = ['**/*.so*']
                lib.dynamicMatchers = []    // NI Libs are not deployed to RIO
                lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:linuxathena@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_chipobject_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:headers@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_netcomm_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.sharedMatchers = ['**/*.so*']
                lib.dynamicMatchers = []    // NI Libs are not deployed to RIO
                lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:linuxathena@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_netcomm_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:headers@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_libraries', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ni_chipobject_binaries' << 'ni_chipobject_headers' << 'ni_netcomm_binaries' << 'ni_netcomm_headers'
                lib.targetPlatform = 'roborio'
                null
            }


            // WPIUTIL
            createWpiLibrary('wpiutil', "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}", 'wpiutil', true)

            // NTCORE
            createWpiLibrary('ntcore', "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}", 'ntcore', true)

            // CSCORE
            createWpiLibrary('cscore', "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}", 'cscore', true)

            // CAMERASERVER
            createWpiLibrary('cameraserver', "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}", 'cameraserver', true)

            // GTEST
            createWpiLibrary('googletest', "edu.wpi.first.thirdparty.frc${wpi.wpilibYear}:googletest:${wpi.googleTestVersion}", 'googletest', true)

            // OPENCV
            libs.create('opencv_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.targetPlatforms << 'desktop'
                lib.headerDirs << ''
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
                lib.configuration = 'native_opencv'
                null
            }

            libs.create('opencv_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryName = 'opencv_binaries'
                lib.dynamicMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.sharedMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip"
                lib.configuration = 'native_opencv'
                null
            }

            if (nativeclassifier != null) {
                libs.create('opencv_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.libraryName = 'opencv_binaries'
                    lib.targetPlatforms = ['desktop']
                    lib.staticMatchers = ['**/*opencv*.lib']
                    lib.sharedMatchers = ['**/*opencv*.so', '**/*opencv*.so.*', '**/*opencv*.dylib']
                    lib.dynamicMatchers = lib.sharedMatchers + '**/*opencv*.dll'
                    lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}@zip"
                    lib.configuration = 'native_opencv_desktop'
                    null
                }
            }

            libs.create('opencv', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'opencv_binaries' << 'opencv_headers'
                lib.targetPlatforms = ['roborio', 'desktop']
                null
            }

            // MASTER WPILIB COMBINED LIB

            libs.create('wpilib', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "cameraserver" << "opencv" << "ni_libraries"
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilibjni', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv" << "ni_libraries"
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilib_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilib'
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "cameraserver" << "opencv"
                clib.targetPlatforms = ['desktop']
                null
            }

            libs.create('wpilibjni_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilibjni'
                clib.libs << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv"
                clib.targetPlatforms = ['desktop']
                null
            }
        }
    }
}
