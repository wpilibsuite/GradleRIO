package jaci.openrio.gradle.wpi.dependencies

import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class WPIJavaDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)

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
            // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
            // libraries, while the -jni ones are standalone (have static libs embedded).
            ["edu.wpi.first.wpilibj:wpilibj-jniShared:${wpi.wpilibVersion}:linuxathena",
             "org.opencv:opencv-jni:${wpi.opencvVersion}:linuxathena",
             "org.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip",              // opencv-jni requires opencv native (opencv is special)
             "edu.wpi.first.hal:hal:${wpi.wpilibVersion}:linuxathena@zip",              // wpilibj-jniShared requires HAL native
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpiutilVersion}:linuxathena@zip", // wpilibj-jniShared requires WPIUtil native
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.ntcoreVersion}:linuxathena@zip",    // wpilibj-jniShared requires NTCore native
             "edu.wpi.first.cscore:cscore-cpp:${wpi.cscoreVersion}:linuxathena@zip"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibJni().each {
                project.dependencies.add("nativeZip", it)
            }
            ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}",
             "edu.wpi.first.ntcore:ntcore-java:${wpi.ntcoreVersion}",
             "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpiutilVersion}",
             "org.opencv:opencv-java:${wpi.opencvVersion}",
             "edu.wpi.first.cscore:cscore-java:${wpi.cscoreVersion}"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.ntcore:ntcore-java:${wpi.ntcoreVersion}:sources",
             "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpiutilVersion}:sources",
             "org.opencv:opencv-java:${wpi.opencvVersion}:sources",
             "edu.wpi.first.cscore:cscore-java:${wpi.cscoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project, WPIExtension wpi) {

        // Java:
        // dependencies {
        //     compile ctre()
        //     compile navx(), bare
        //
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     nativeLib  fileTree(dir: 'libs', include: '**/*.so')
        // }

        // Bare is passed as a configuration closure to navx (and other dependencies) in order to prevent
        // transitive wpilib dependencies that could potentially conflict with our own versions.
        project.dependencies.ext.bare = {
            exclude module: 'athena'
            exclude module: 'wpilibj-java'
        }

        project.dependencies.ext.ctreJni = {
            "openrio.mirror.third.ctre:CTRE-phoenix-java:${wpi.ctreVersion}:native@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["openrio.mirror.third.ctre:CTRE-phoenix-java:${wpi.ctreVersion}"]
        }


        project.dependencies.ext.navx = {
            ["com.kauailabs.navx.frc:navx_frc:${wpi.navxVersion}"]
        }
    }
}
