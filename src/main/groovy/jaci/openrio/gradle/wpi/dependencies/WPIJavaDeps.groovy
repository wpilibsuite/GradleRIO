package jaci.openrio.gradle.wpi.dependencies

import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class WPIJavaDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)

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
}
