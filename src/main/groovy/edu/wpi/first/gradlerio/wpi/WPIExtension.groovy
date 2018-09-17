package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven/release) libs
    String wpilibVersion = "2018.4.1"
    String ntcoreVersion = "4.1.0"
    String opencvVersion = "3.2.0"
    String cscoreVersion = "1.3.0"
    String wpiutilVersion = "3.2.0"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "3.0.0"
    String shuffleboardVersion = "1.3.1"
    String outlineViewerVersion = "2.0.7"
    String robotBuilderVersion = "3.0.1"

    // WPILib Toolchain (first.wpi.edu/FRC/roborio/toolchains) version
    String toolchainVersion = "2018-5.5"
    String toolchainVersionLow = "5.5"
    String toolchainVersionHigh = "5.5"

    String frcYear = '2018'

    final Project project

    WPIExtension(Project project) {
        this.project = project
    }

    private String frcHomeCache

    String getFrcHome() {
        if (frcHomeCache != null) {
            return this.frcHomeCache
        }
        def frcHome = System.getenv("FRC_${this.frcYear}_HOME")

        if (frcHome == null) {
            if (OperatingSystem.current().isWindows()) {
                frcHome = "C:\\Users\\Public\\frc${this.frcYear}"
            } else {
                def userFolder = System.getProperty("user.home")
                frcHome = new File(userFolder, "wpilib${this.frcYear}").toString()
            }
            // TODO Figure out how to sent the frc home variable
        }
        frcHomeCache = frcHome
        return frcHomeCache
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

            "smartDashboardVersion" : new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
            "shuffleboardVersion" : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),
            "outlineViewerVersion" : new Tuple("OutlineViewer", outlineViewerVersion, "outlineviewer"),
            "robotBuilderVersion" : new Tuple("RobotBuilder", robotBuilderVersion, "robotbuilder"),

            "toolchainVersion" : new Tuple("Toolchain", toolchainVersion, "toolchain"),
        ]
    }
}
