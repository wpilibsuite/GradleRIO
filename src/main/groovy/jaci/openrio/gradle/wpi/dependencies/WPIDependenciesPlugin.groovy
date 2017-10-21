package jaci.openrio.gradle.wpi.dependencies

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

class WPIDependenciesPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)

        project.repositories.maven { repo ->
            repo.name = "WPI"
            repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven { repo ->
            repo.name = "Jaci"
            repo.url = "http://dev.imjac.in/maven/"
        }

        apply_wpi_dependencies(project, project.extensions.getByType(WPIExtension))
        apply_third_party_drivers(project, project.extensions.getByType(WPIExtension))
    }

    void apply_wpi_dependencies(Project project, WPIExtension wpi) {

        // Add WPILib to your project:
        // Java:
        // dependencies {
        //     compile wpilib()
        // }

        project.dependencies.ext.wpilibJni = {
            def l = ["edu.wpi.first.wpilibj:athena-jni:${wpi.wpilibVersion}",
             "org.opencv:opencv-jni:${wpi.opencvVersion}:${wpi.opencvVersion == "3.1.0" ? "linux-arm" : "linuxathena"}",
             "edu.wpi.first.wpilib:athena-runtime:${wpi.wpilibVersion}@zip",
             "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:athena-uberzip@zip"]
            if (wpi.opencvVersion == "3.1.0") l << "org.opencv:opencv-natives:${wpi.opencvVersion}:linux-arm"
            l
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibJni().each {
                project.dependencies.add("nativeZip", it)
            }
            ["edu.wpi.first.wpilibj:athena:${wpi.wpilibVersion}",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:arm",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:desktop",
             "org.opencv:opencv-java:${wpi.opencvVersion}",
             "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:arm"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:athena:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project, WPIExtension wpi) {

        // Java:
        // dependencies {
        //     compile ctre()
        //     compile navx()
        //
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     nativeLib  fileTree(dir: 'libs', include: '**/*.so')
        // }

        project.dependencies.ext.ctreJni = {
            "thirdparty.frc.ctre:Toolsuite-Zip:${wpi.ctreVersion}@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${wpi.ctreVersion}"]
        }
        project.dependencies.ext.navx = {
            ["thirdparty.frc.kauai:Navx-Java:${wpi.navxVersion}"]
        }
    }

    static class WPIDepRules extends RuleSource {
        @Mutate
        void addWPILibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def common = { NativeLib lib ->
                lib.targetPlatform = 'roborio'
                lib.headerDirs = ['include']
                lib.staticMatchers = ['**/*.a']
            }

            libs.create('wpilibc', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryNames = ['wpi']
                lib.sharedMatchers = ["**/libwpilibc.so"]
                lib.maven = "edu.wpi.first.wpilibc:athena:${wpi.wpilibVersion}"
            }

            libs.create('hal', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libHALAthena.so']
                lib.sharedMatchers = ["**/libHALAthena.so"]
                lib.maven = "edu.wpi.first.wpilib:hal:${wpi.wpilibVersion}"
            }

            libs.create('ntcore', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libntcore.so']
                lib.sharedMatchers = ["**/libntcore.so"]
                lib.maven = "edu.wpi.first.wpilib.networktables.cpp:NetworkTables:${wpi.ntcoreVersion}:arm@zip"
            }

            libs.create('wpiutil', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libwpiutil.so']
                lib.sharedMatchers = ["**/libwpiutil.so"]
                lib.maven = "edu.wpi.first.wpilib:wpiutil:${wpi.wpiutilVersion}:arm@zip"
            }

            libs.create('cscore', NativeLib) { NativeLib lib ->
                common(lib)
                lib.libraryMatchers = ['**/libopencv*.so.3.1', '**/libcscore.so']
                lib.sharedMatchers = ["**/libopencv*.so.3.1", "**/libcscore.so"]
                lib.maven = "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:athena-uberzip@zip"
            }

            libs.create('wpilib', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc" << "hal" << "ntcore" << "wpiutil" << "cscore"
                clib.targetPlatform = 'roborio'
            }

            // CTRE
            libs.create('ctre', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs = ['cpp/include']
                lib.maven = "thirdparty.frc.ctre:Toolsuite-Zip:${wpi.ctreVersion}@zip"
            }

            // NavX
            libs.create('navx', NativeLib) { NativeLib lib ->
                common(lib)
                lib.headerDirs = ['roborio/cpp/include']
                lib.maven = "thirdparty.frc.kauai:Navx-Zip:${wpi.navxVersion}@zip"
            }
        }
    }
}
