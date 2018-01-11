package jaci.openrio.gradle.wpi

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Project

import java.security.MessageDigest

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven/release) libs
    String wpilibVersion = "2018.1.1"
    String ntcoreVersion = "4.0.0"
    String opencvVersion = "3.2.0"
    String cscoreVersion = "1.1.0"
    String wpiutilVersion = "3.0.0"

    // Third Party (dev.imjac.in/maven/thirdparty) libs
    String ctreVersion = "5.1.2.1"
    String navxVersion = "3.0.342"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "2.0.4"
    String shuffleboardVersion = "1.0.0"

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
            "ntcoreVersion" : new Tuple("NTCore", ntcoreVersion, "ntcore"),
            "opencvVersion" : new Tuple("OpenCV", opencvVersion, "opencv"),
            "cscoreVersion" : new Tuple("CSCore", cscoreVersion, "cscore"),

            "wpiutilVersion" : new Tuple("WPIUtil (C++)", wpiutilVersion, "wpiutil"),

//            "ctreVersion" : new Tuple("CTRE", ctreVersion, "ctre"),
            "navxVersion" : new Tuple("NavX", navxVersion, "navx"),

            "smartDashboardVersion" : new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
            "shuffleboardVersion" : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),

            "toolchainVersion" : new Tuple("Toolchain", toolchainVersion, "toolchain")
        ]
    }
}