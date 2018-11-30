package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class WPIJavaDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)

        def wpiext = project.extensions.getByType(WPIExtension)
        apply_wpi_dependencies(project, wpiext)
    }

    void apply_wpi_dependencies(Project project, WPIExtension wpi) {

        // Add WPILib to your project:
        // Java:
        // dependencies {
        //     compile wpilib()
        // }

        project.dependencies.ext.wpilibDesktopJni = {
             ["edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${wpi.platforms.desktop}debug@zip",
             "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}debug@zip",
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}debug@zip",
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}debug@zip",
             "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}debug@zip"]
        }

        project.dependencies.ext.wpilibJni = {
            // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
            // libraries, while the -jni ones are standalone (have static libs embedded).
            ["edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathenadebug@zip",
             "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:linuxathenadebug@zip",
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:linuxathenadebug@zip",
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:linuxathenadebug@zip",
             "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:linuxathenadebug@zip"]
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
             "edu.wpi.first.thirdparty.frc2019.opencv:opencv-java:${wpi.opencvVersion}",
             "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}",
             "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}",
             "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.thirdparty.frc2019.opencv:opencv-java:${wpi.opencvVersion}:sources",
             "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}:sources"]
        }
    }
}
