package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.os.OperatingSystem
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

        @Mutate
        void addWPILibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def common = { NativeLib lib ->
                lib.targetPlatforms = ['roborio']
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.staticMatchers = []
            }

            def nativeclassifier = (
                    OperatingSystem.current().isWindows() ?
                    System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                    OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                    null
            )

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
                        lib.staticMatchers = ["**/*${libname}.lib".toString()]
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
            createWpiLibrary('wpilibc', "edu.wpi.first.wpilibc:wpilibc:${wpi.wpilibVersion}", 'wpilibc', true)

            // HAL
            createWpiLibrary('hal', "edu.wpi.first.hal:hal:${wpi.wpilibVersion}", 'wpiHal', true)


            // NI LIBS
            libs.create('ni_libraries_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.sharedMatchers = ['**/*.so*']
                lib.dynamicMatchers = []    // NI Libs are not deployed to RIO
                lib.maven = "edu.wpi.first.ni-libraries:ni-libraries:${wpi.wpilibVersion}:linuxathena@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_libraries_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:ni-libraries:${wpi.wpilibVersion}:headers@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_libraries', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ni_libraries_binaries' << 'ni_libraries_headers'
                lib.targetPlatform = 'roborio'
                null
            }


            // WPIUTIL
            createWpiLibrary('wpiutil', "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpiutilVersion}", 'wpiutil', true)

            // NTCORE
            createWpiLibrary('ntcore', "edu.wpi.first.ntcore:ntcore-cpp:${wpi.ntcoreVersion}", 'ntcore', true)

            // CSCORE
            createWpiLibrary('cscore', "edu.wpi.first.cscore:cscore-cpp:${wpi.cscoreVersion}", 'cscore', true)

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
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv" << "ni_libraries"
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
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv"
                clib.targetPlatforms = ['desktop']
                null
            }

            libs.create('wpilibjni_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilibjni'
                clib.libs << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv"
                clib.targetPlatforms = ['desktop']
                null
            }

            // CTRE

            libs.create('ctre_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryName = "ctre_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.ctre:CTRE-phoenix-cpp:${wpi.ctreVersion}@zip"
                lib.configuration = 'native_ctre'
                null
            }

            libs.create('ctre_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.ctre:CTRE-phoenix-cpp:${wpi.ctreVersion}:headers@zip"
                lib.configuration = 'native_ctre'
                null
            }

            libs.create('ctre', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ctre_binaries' << 'ctre_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // CTRE (Legacy)

            libs.create('ctre_legacy_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryName = "ctre_legacy_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.ctre:CTRE-toolsuite-cpp:${wpi.ctreLegacyVersion}@zip"
                lib.configuration = 'native_ctre_legacy'
                null
            }

            libs.create('ctre_legacy_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.ctre:CTRE-toolsuite-cpp:${wpi.ctreLegacyVersion}:headers@zip"
                lib.configuration = 'native_ctre_legacy'
                null
            }

            libs.create('ctre_legacy', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ctre_legacy_binaries' << 'ctre_legacy_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // NAVX

            libs.create('navx_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryName = "navx_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.kauailabs:navx-cpp:${wpi.navxVersion}@zip"
                lib.configuration = 'native_navx'
                null
            }

            libs.create('navx_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.kauailabs:navx-cpp:${wpi.navxVersion}:headers@zip"
                lib.configuration = 'native_navx'
                null
            }

            libs.create('navx', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'navx_binaries' << 'navx_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // OpenRIO

            libs.create('pathfinder_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryName = 'pathfinder_binaries'
                lib.staticMatchers = ['*.a']
                lib.maven = "jaci.pathfinder:Pathfinder-Core:${wpi.pathfinderVersion}:athena@zip"
                lib.configuration = 'native_pathfinder'
                null
            }

            libs.create('pathfinder_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "jaci.pathfinder:Pathfinder-Core:${wpi.pathfinderVersion}:headers@zip"
                lib.configuration = 'native_pathfinder'
                null
            }

            libs.create('pathfinder', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'pathfinder_binaries' << 'pathfinder_headers'
                lib.targetPlatforms = ['roborio']
                null
            }
        }
    }
}
