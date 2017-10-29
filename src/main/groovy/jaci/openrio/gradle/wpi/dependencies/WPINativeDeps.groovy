package jaci.openrio.gradle.wpi.dependencies

import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import jaci.openrio.gradle.wpi.WPIExtension
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
        @Mutate
        void addWPILibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def common = { NativeLib lib ->
                lib.targetPlatform = 'roborio'
                lib.headerDirs = []
                lib.staticMatchers = ['**/*.a']
            }

            // WPILIB C

            libs.create('wpilibc_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libwpilibc.so']
                lib.sharedMatchers = ["**/libwpilibc.so"]
                lib.maven = "edu.wpi.first.wpilibc:wpilibc:${wpi.wpilibVersion}:linuxathena@zip"
            }

            libs.create('wpilibc_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.wpilibc:wpilibc:${wpi.wpilibVersion}:headers@zip"
            }

            libs.create('wpilibc', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << "wpilibc_binaries" << "wpilibc_headers"
                lib.targetPlatform = 'roborio'
            }

            // HAL

            libs.create('hal_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libwpiHal.so']
                lib.sharedMatchers = ["**/libwpiHal.so"]
                lib.maven = "edu.wpi.first.hal:hal:${wpi.wpilibVersion}:linuxathena@zip"
            }

            libs.create('hal_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.hal:hal:${wpi.wpilibVersion}:headers@zip"
            }

            libs.create('hal', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'hal_binaries' << 'hal_headers'
                lib.targetPlatform = 'roborio'
            }

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

            libs.create('wpiutil_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libwpiutil.so']
                lib.sharedMatchers = ["**/libwpiutil.so"]
                lib.maven = "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpiutilVersion}:linuxathena@zip"
            }

            libs.create('wpiutil_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpiutilVersion}:headers@zip"
            }

            libs.create('wpiutil', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'wpiutil_binaries' << 'wpiutil_headers'
                lib.targetPlatform = 'roborio'
            }

            // NTCORE

            libs.create('ntcore_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libntcore.so']
                lib.sharedMatchers = ["**/libntcore.so"]
                lib.maven = "edu.wpi.first.ntcore:ntcore-cpp:${wpi.ntcoreVersion}:linuxathena@zip"
            }

            libs.create('ntcore_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ntcore:ntcore-cpp:${wpi.ntcoreVersion}:headers@zip"
            }

            libs.create('ntcore', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ntcore_binaries' << 'ntcore_headers'
                lib.targetPlatform = 'roborio'
            }

            // CSCORE

            libs.create('cscore_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libcscore.so']
                lib.sharedMatchers = ["**/libcscore.so"]
                lib.maven = "edu.wpi.first.cscore:cscore-cpp:${wpi.cscoreVersion}:linuxathena@zip"
            }

            libs.create('cscore_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.cscore:cscore-cpp:${wpi.cscoreVersion}:headers@zip"
            }

            libs.create('cscore', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'cscore_binaries' << 'cscore_headers'
                lib.targetPlatform = 'roborio'
            }

            // OPENCV

            libs.create('opencv_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.sharedMatchers = ['**/libopencv*.so.*', '**/libopencv*.so']
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip"
            }

            libs.create('opencv_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs << ''
                lib.maven = "org.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
            }

            libs.create('opencv', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'opencv_binaries' << 'opencv_headers'
                lib.targetPlatform = 'roborio'
            }

            // MASTER WPILIB COMBINED LIB

            libs.create('wpilib', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv" << "ni_libraries"
                clib.targetPlatform = 'roborio'
            }

            // TODO: CTRE uses phoenix instead of toolsuite now?
            // CTRE
//            libs.create('ctre', NativeLib) { NativeLib lib ->
//                common(lib)
//                lib.headerDirs = ['cpp/include']
//                lib.libraryMatchers = ['cpp/**/*.a']
//                lib.maven = "thirdparty.frc.ctre:Toolsuite-Zip:${wpi.ctreVersion}@zip"
//            }

            // TODO: NavX is not yet stable for 2018
            // NavX
//            libs.create('navx', NativeLib) { NativeLib lib ->
//                common(lib)
//                lib.headerDirs = ['roborio/cpp/include']
//                lib.libraryMatchers = ['roborio/**/*.a']
//                lib.maven = "thirdparty.frc.kauai:Navx-Zip:${wpi.navxVersion}@zip"
//            }
        }
    }
}
