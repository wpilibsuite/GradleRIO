package jaci.openrio.gradle.wpi.dependencies

import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class WPIJavaDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)

        def wpiext = project.extensions.getByType(WPIExtension)
        apply_wpi_dependencies(project, wpiext)
        apply_third_party_drivers(project, wpiext)
        apply_openrio(project, wpiext)
    }

    void apply_wpi_dependencies(Project project, WPIExtension wpi) {

        // Add WPILib to your project:
        // Java:
        // dependencies {
        //     compile wpilib()
        // }

        def nativeclassifier = (
            OperatingSystem.current().isWindows() ?
            System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
            OperatingSystem.current().isLinux() ? "linuxx86-64" :
            null
        )

        project.dependencies.ext.wpilibDesktopJni = {
             ["org.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}@zip",              // opencv-jni requires opencv native (opencv is special)
             "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${nativeclassifier}@zip",              // wpilibj-jniShared requires HAL native
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${nativeclassifier}@zip", // wpilibj-jniShared requires WPIUtil native
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${nativeclassifier}@zip",    // wpilibj-jniShared requires NTCore native
             "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${nativeclassifier}@zip",
             "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}:${nativeclassifier}@zip"]
        }

        project.dependencies.ext.wpilibJni = {
            // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
            // libraries, while the -jni ones are standalone (have static libs embedded).
             ["org.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip",              // opencv-jni requires opencv native (opencv is special)
             "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:linuxathena@zip",              // wpilibj-jniShared requires HAL native
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:linuxathena@zip", // wpilibj-jniShared requires WPIUtil native
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:linuxathena@zip",    // wpilibj-jniShared requires NTCore native
             "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:linuxathena@zip",
             "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}:linuxathena@zip"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibJni().each {
                project.dependencies.add("nativeZip", it)
            }
            project.dependencies.ext.wpilibDesktopJni().each {
                project.dependencies.add("nativeDesktopZip", it)
            }
            ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}",
             "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}",
             "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}",
             "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}",
             "org.opencv:opencv-java:${wpi.opencvVersion}",
             "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}",
             "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}:sources",
             "org.opencv:opencv-java:${wpi.opencvVersion}:sources",
             "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}:sources"]
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

        // TODO: 2019
        // Bare is passed as a configuration closure to navx (and other dependencies) in order to prevent
        // transitive wpilib dependencies that could potentially conflict with our own versions.
//        project.dependencies.ext.bare = {
//            exclude module: 'athena'
//            exclude module: 'wpilibj-java'
//        }

        project.dependencies.ext.ctreJni = {
            "openrio.mirror.third.ctre:CTRE-phoenix-java:${wpi.ctreVersion}:native@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["openrio.mirror.third.ctre:CTRE-phoenix-java:${wpi.ctreVersion}"]
        }

        project.dependencies.ext.ctreLegacyJni = {
            "openrio.mirror.third.ctre:CTRE-toolsuite-java:${wpi.ctreLegacyVersion}:native@zip"
        }

        project.dependencies.ext.ctreLegacy = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreLegacyJni())
            ["openrio.mirror.third.ctre:CTRE-toolsuite-java:${wpi.ctreLegacyVersion}"]
        }

        project.dependencies.ext.navx = {
            ["openrio.mirror.third.kauailabs:navx-java:${wpi.navxVersion}"]
        }
    }

    void apply_openrio(Project project, WPIExtension wpi) {
        project.dependencies.ext.pathfinderJni = {
            "jaci.pathfinder:Pathfinder-JNI:1.8:athena@zip"
        }
        project.dependencies.ext.pathfinder = {
            project.dependencies.add("nativeZip", project.dependencies.ext.pathfinderJni())
            ["jaci.pathfinder:Pathfinder-Java:${wpi.pathfinderVersion}"]
        }

        project.dependencies.ext.openrio = [
            powerup: [
                matchData: { ["openrio.powerup:MatchData:${wpi.openrioMatchDataVersion}"] }
            ]
        ]
    }
}
