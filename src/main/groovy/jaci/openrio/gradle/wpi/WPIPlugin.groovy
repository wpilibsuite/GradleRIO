package jaci.openrio.gradle.frc

import org.gradle.api.*;
import groovy.util.*;

class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("wpi", WPIExtension, project)

        project.repositories.maven {
            name = "WPI"
            url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven {
            name = "Jaci"
            url = "http://dev.imjac.in/maven/"
        }

        // This is needed because Gradle will not download dependencies
        // until they are referenced, so we need to do this before build
        // as otherwise deploy will not work (will try to download while
        // not connected to the internet)
        project.task("resolveNativeDeps") {
            group "GradleRIO"
            description "Resolve Dependencies from Maven"
            project.tasks.getByName("build").dependsOn it
            doLast {
                def conf = [project.configurations.native, project.configurations.nativeJar, project.configurations.nativeZip]
                conf.each { c -> 
                    c.dependencies.findAll { it != null }.collect {
                        def libfile = c.files(it)[0]
                    }
                }
            }
        }

        apply_wpi_dependencies(project);
        apply_third_party_drivers(project);
    }

    void apply_wpi_dependencies(Project project) {
        project.repositories.maven {
            name = "WPI"
            url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        // Add WPILib to your project:
        // dependencies {
        //     compile wpilib()
        // }

        project.dependencies.ext.wpilibNative = {
            "edu.wpi.first.wpilibj:athena-jni:${project.wpi.wpilibVersion}"
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.add("nativeJar", project.dependencies.ext.wpilibNative())
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:arm",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:desktop"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project) {

        // dependencies {
        //     compile talonSrx()
        //     
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     native  fileTree(dir: 'libs', include: '**/*.so')
        // }

        project.dependencies.ext.talonSrxJni = {
            "thirdparty.frc.ctre:Toolsuite-Zip:${project.wpi.talonSrxVersion}@zip"
        }

        project.dependencies.ext.talonSrx = {
            project.dependencies.add("nativeZip", project.dependencies.ext.talonSrxJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${project.wpi.talonSrxVersion}"]
        }
    }
}