package jaci.openrio.gradle.wpi.dependencies

import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import jaci.openrio.gradle.wpi.WPIExtension
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
                lib.staticMatchers = []
            }

            def native64classifier = (
                    OperatingSystem.current().isWindows() ? "windowsx86-64" :
                    OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                    null
            )

            def createWpiLibrary = { String name, String mavenBase, String libname, boolean supportNative ->
                libs.create("${name}_headers", NativeLib) { NativeLib lib ->
                    common(lib)
                    if (supportNative)
                        lib.targetPlatforms << 'any64'
                    lib.headerDirs << ''
                    lib.maven = "${mavenBase}:headers@zip"
                }

                libs.create("${name}_athena", NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.mainLibraryName = "${name}_binaries"
                    lib.sharedMatchers = ["**/lib${libname}.so".toString()]
                    lib.libraryMatchers = lib.sharedMatchers
                    lib.maven = "${mavenBase}:linuxathena@zip"
                }

                if (supportNative && native64classifier != null) {
                    libs.create("${name}_native", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.mainLibraryName = "${name}_binaries"
                        lib.targetPlatforms = ['any64']
                        lib.staticMatchers = ["**/*${libname}.lib".toString()]
                        lib.sharedMatchers = ["**/*${libname}.so".toString(), "**/*${libname}.dylib".toString()]
                        lib.libraryMatchers = lib.sharedMatchers + "windows/x86-64/shared/${libname}.dll".toString()
                        lib.maven = "${mavenBase}:${native64classifier}@zip"
                    }
                }

                libs.create(name, CombinedNativeLib) { CombinedNativeLib lib ->
                    lib.libs << "${name}_binaries".toString() << "${name}_headers".toString()
                    lib.targetPlatforms = ['roborio']
                    if (supportNative)
                        lib.targetPlatforms << 'any64'
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
                lib.libraryMatchers = ['**/*.so*']
                lib.sharedMatchers = ['**/*.so*']
                lib.maven = "edu.wpi.first.ni-libraries:ni-libraries:${wpi.wpilibVersion}:linuxathena@zip"
            }

            libs.create('ni_libraries_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:ni-libraries:${wpi.wpilibVersion}:headers@zip"
            }

            libs.create('ni_libraries', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ni_libraries_binaries' << 'ni_libraries_headers'
                lib.targetPlatform = 'roborio'
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
                lib.targetPlatforms << 'any64'
                lib.headerDirs << ''
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
            }

            libs.create('opencv_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.mainLibraryName = 'opencv_binaries'
                lib.libraryMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.sharedMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip"
            }

            if (native64classifier != null) {
                libs.create('opencv_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.mainLibraryName = 'opencv_binaries'
                    lib.targetPlatforms = ['any64']
                    lib.staticMatchers = ['**/*opencv*.lib']
                    lib.sharedMatchers = ['**/*opencv*.so', '**/*opencv*.so.*', '**/*opencv*.dylib']
                    lib.libraryMatchers = lib.sharedMatchers + '**/*opencv*.dll'
                    lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:${native64classifier}@zip"
                }
            }

            libs.create('opencv', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'opencv_binaries' << 'opencv_headers'
                lib.targetPlatforms = ['roborio', 'any64']
                null
            }

            // MASTER WPILIB COMBINED LIB

            libs.create('wpilib', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv" << "ni_libraries"
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilib_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.mainLibraryName = 'wpilib'
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv"
                clib.targetPlatforms = ['any64']
                null
            }

            // CTRE

            libs.create('ctre_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.mainLibraryName = "ctre_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.ctre:CTRE-phoenix-cpp:${wpi.ctreVersion}@zip"
            }

            libs.create('ctre_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.ctre:CTRE-phoenix-cpp:${wpi.ctreVersion}:headers@zip"

            }

            libs.create('ctre', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ctre_binaries' << 'ctre_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // CTRE (Legacy)

            libs.create('ctre_legacy_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.mainLibraryName = "ctre_legacy_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.ctre:CTRE-toolsuite-cpp:${wpi.ctreLegacyVersion}@zip"
            }

            libs.create('ctre_legacy_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.ctre:CTRE-toolsuite-cpp:${wpi.ctreLegacyVersion}:headers@zip"

            }

            libs.create('ctre_legacy', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ctre_legacy_binaries' << 'ctre_legacy_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // NAVX

            libs.create('navx_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.mainLibraryName = "navx_binaries"
                lib.headerDirs = []
                lib.staticMatchers = ['*.a']
                lib.maven = "openrio.mirror.third.kauailabs:navx-cpp:${wpi.navxVersion}@zip"
            }

            libs.create('navx_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "openrio.mirror.third.kauailabs:navx-cpp:${wpi.navxVersion}:headers@zip"

            }

            libs.create('navx', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'navx_binaries' << 'navx_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            // OpenRIO

            libs.create('pathfinder_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.mainLibraryName = 'pathfinder_binaries'
                lib.staticMatchers = ['*.a']
                lib.maven = "jaci.pathfinder:Pathfinder-Core:${wpi.pathfinderVersion}:athena@zip"
            }

            libs.create('pathfinder_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "jaci.pathfinder:Pathfinder-Core:${wpi.pathfinderVersion}:headers@zip"
            }

            libs.create('pathfinder', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'pathfinder_binaries' << 'pathfinder_headers'
                lib.targetPlatforms = ['roborio']
                null
            }

            libs.create('openrio.powerup.matchData', NativeLib) { NativeLib lib ->
                lib.targetPlatforms = ['roborio']
                lib.headerDirs = ['']
                lib.maven = "openrio.powerup:MatchData:${wpi.openrioMatchDataVersion}:headers@zip"
            }
        }
    }
}
