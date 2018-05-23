package jaci.openrio.gradle.wpi

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Project

import java.security.MessageDigest

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven/release) libs
    String wpilibVersion = "2018.424242.+"
    String opencvVersion = "3.2.0"

    // Third Party (dev.imjac.in/maven/thirdparty) libs
    String ctreVersion = "5.2.1.1"
    String ctreLegacyVersion = "4_Legacy"
    String navxVersion = "3.0.346"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "3.0.0"
    String shuffleboardVersion = "1.2.1"

    // WPILib Toolchain (first.wpi.edu/FRC/roborio/toolchains) version
    String toolchainVersion = "2018-5.5"

    // OpenRIO Dependencies
    String pathfinderVersion = "1.8"
    String openrioMatchDataVersion = "2018.01.07"

    final Project project

    WPIExtension(Project project) {
        this.project = project
    }

    Map<String, Tuple> versions() {
        // Format:
        // property: [ PrettyName, Version, RecommendedKey ]
        return [
            "wpilibVersion" : new Tuple("WPILib", wpilibVersion, "wpilib"),
            "opencvVersion" : new Tuple("OpenCV", opencvVersion, "opencv"),

            "ctreVersion" : new Tuple("CTRE", ctreVersion, "ctre"),
            "ctreLegacyVersion": new Tuple("CTRE (Legacy)", ctreLegacyVersion, "ctreLegacy"),
            "navxVersion" : new Tuple("NavX", navxVersion, "navx"),

            "smartDashboardVersion" : new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
            "shuffleboardVersion" : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),

            "toolchainVersion" : new Tuple("Toolchain", toolchainVersion, "toolchain"),

            "pathfinderVersion": new Tuple("Pathfinder", pathfinderVersion, "pathfinder"),
            "openrioMatchDataVersion" : new Tuple("OpenRIO:MatchData", openrioMatchDataVersion, "openrioMatchData")
        ]
    }
}
