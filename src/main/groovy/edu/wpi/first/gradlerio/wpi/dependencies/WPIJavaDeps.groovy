package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

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

        def nativeclassifier = (
            OperatingSystem.current().isWindows() ?
            System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
            OperatingSystem.current().isLinux() ? "linuxx86-64" :
            null
        )

        project.dependencies.ext.wpilibDesktopJni = {
            ["edu.wpi.first.wpilibj:wpilibj-jniShared:${wpi.wpilibVersion}:${nativeclassifier}",
             "org.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}@zip",              // opencv-jni requires opencv native (opencv is special)
             "edu.wpi.first.hal:hal:${wpi.wpilibVersion}:${nativeclassifier}@zip",              // wpilibj-jniShared requires HAL native
             "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpiutilVersion}:${nativeclassifier}@zip", // wpilibj-jniShared requires WPIUtil native
             "edu.wpi.first.ntcore:ntcore-cpp:${wpi.ntcoreVersion}:${nativeclassifier}@zip",    // wpilibj-jniShared requires NTCore native
             "edu.wpi.first.cscore:cscore-cpp:${wpi.cscoreVersion}:${nativeclassifier}@zip"]
        }

        project.dependencies.ext.wpilibJni = {
            // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
            // libraries, while the -jni ones are standalone (have static libs embedded).
            ["edu.wpi.first.wpilibj:wpilibj-jniShared:${wpi.wpilibVersion}:linuxathena",
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
            project.dependencies.ext.wpilibDesktopJni().each {
                project.dependencies.add("nativeDesktopZip", it)
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
}
