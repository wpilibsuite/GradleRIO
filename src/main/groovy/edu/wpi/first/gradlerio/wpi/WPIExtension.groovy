package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven/release) libs
    String wpilibVersion = "2018.4.1"
    String ntcoreVersion = "4.1.0"
    String opencvVersion = "3.2.0"
    String cscoreVersion = "1.3.0"
    String wpiutilVersion = "3.2.0"

    // Third Party (dev.imjac.in/maven/thirdparty) libs
    String ctreVersion = "5.5.1.0"
    String ctreLegacyVersion = "4_Legacy"
    String navxVersion = "3.0.348"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "3.0.0"
    String shuffleboardVersion = "1.3.1"

    // WPILib Toolchain (first.wpi.edu/FRC/roborio/toolchains) version
    String toolchainVersion = "2018-5.5"
    String toolchainVersionLow = "5.5"
    String toolchainVersionHigh = "5.5"

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
