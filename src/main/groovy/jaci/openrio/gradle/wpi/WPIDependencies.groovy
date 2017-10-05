package jaci.openrio.gradle.wpi

import org.gradle.api.Project
import org.gradle.api.Task

import java.security.MessageDigest

class WPIDependencies {
    void apply(Project project) {
        project.repositories.maven { repo ->
            repo.name = "WPI"
            repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven { repo ->
            repo.name = "Jaci"
            repo.url = "http://dev.imjac.in/maven/"
        }

        // This is needed because Gradle will not download dependencies
        // until they are referenced, so we need to do this before build
        // as otherwise frc will not work (will try to download while
        // not connected to the internet)
        def resolveDepsTask = project.task("resolveNativeDeps") { Task task ->
            task.group = "GradleRIO"
            task.description = "Resolve Dependencies from Maven"
            task.doLast {
                def conf = [project.configurations.nativeLib, project.configurations.nativeZip]
                conf.each { c ->
                    c.dependencies.findAll { it != null }.collect {
                        def libfile = c.files(it)[0]
                    }
                }
            }
        }
        project.tasks.matching { t -> t.name == "build" }.whenTaskAdded { t -> t.dependsOn resolveDepsTask }

        apply_wpi_dependencies(project);
        apply_third_party_drivers(project);
    }


    void apply_wpi_dependencies(Project project) {

        // Add WPILib to your project:
        // dependencies {
        //     compile wpilib()
        // }

        project.dependencies.ext.wpilibNative = {
            ["edu.wpi.first.wpilibj:athena-jni:${project.wpi.wpilibVersion}",
             "org.opencv:opencv-jni:${project.wpi.opencvVersion}:${project.wpi.opencvVersion == "3.1.0" ? "linux-arm" : "linuxathena"}",
             "edu.wpi.first.wpilib:athena-runtime:${project.wpi.wpilibVersion}@zip",
             "edu.wpi.cscore.java:cscore:${project.wpi.cscoreVersion}:athena-uberzip@zip"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibNative().each {
                project.dependencies.add("nativeZip", it)
            }
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:arm",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:desktop",
             "org.opencv:opencv-java:${project.wpi.opencvVersion}",
             "edu.wpi.cscore.java:cscore:${project.wpi.cscoreVersion}:arm"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project) {

        // dependencies {
        //     compile ctre()
        //     compile navx()
        //
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     nativeLib  fileTree(dir: 'libs', include: '**/*.so')
        // }

        project.dependencies.ext.ctreJni = {
            "thirdparty.frc.ctre:Toolsuite-Zip:${project.wpi.ctreVersion}@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${project.wpi.ctreVersion}"]
        }

        project.dependencies.ext.navx = {
            ["thirdparty.frc.kauai:Navx-Java:${project.wpi.navxVersion}"]
        }
    }
}
