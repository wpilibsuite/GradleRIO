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

        project.configurations.maybeCreate("native")

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
            ["edu.wpi.first.wpilibj:athena-jni:${project.wpi.wpilibVersion}"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.add("native", wpilibNative())
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
        //     compile deviceLibraries()        // This will include myLibrary above
        // }

        project.dependencies.ext.talonSrxJni = {
            ["thirdparty.frc.ctre:Toolsuite-JNI:${project.wpi.talonSrxVersion}"]
        }

        project.dependencies.ext.talonSrx = {
            project.dependencies.add("native", talonSrxJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${project.wpi.talonSrxVersion}"]
        }
    }
}