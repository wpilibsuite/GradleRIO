package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    String wpilibVersion = "2018.4.1-20180921151738-1194-gf89274f"
    String niLibrariesVersion = "2018.17.1"
    String opencvVersion = "3.2.0"

    String wpilibYear = '2018'

    String googleTestVersion = "1.8.0-1-4e4df22"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "3.0.0"
    String shuffleboardVersion = "1.3.1"
    String outlineViewerVersion = "2.0.7"
    String robotBuilderVersion = "3.0.1"

    // WPILib Toolchain (first.wpi.edu/FRC/roborio/toolchains) version
    String toolchainVersion = "2018-5.5"
    String toolchainVersionLow = "5.5"
    String toolchainVersionHigh = "5.5"

    boolean developmentBranch

    String frcYear = '2018'

    final Project project
    final String nativeClassifier
    final String toolsClassifier

    WPIExtension(Project project) {
        this.project = project
        this.nativeClassifier = (
                OperatingSystem.current().isWindows() ?
                        System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                        OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                        null
        )
        this.toolsClassifier = (
                OperatingSystem.current().isWindows() ?
                        System.getProperty("os.arch") == 'amd64' ? 'win64' : 'win32' :
                        OperatingSystem.current().isMacOsX() ? "mac64" :
                                OperatingSystem.current().isLinux() ? "linux64" :
                                        null
        )
        this.developmentBranch = true
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
                "wpilibVersion"        : new Tuple("WPILib", wpilibVersion, "wpilib"),
                "opencvVersion"        : new Tuple("OpenCV", opencvVersion, "opencv"),
                "wpilibYear"           : new Tuple("WPILib Year", wpilibYear, "wpilibYear"),
                "googleTestVersion"    : new Tuple("Google Test", googleTestVersion, "googleTest"),

                "smartDashboardVersion": new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
                "shuffleboardVersion"  : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),
                "outlineViewerVersion" : new Tuple("OutlineViewer", outlineViewerVersion, "outlineviewer"),
                "robotBuilderVersion"  : new Tuple("RobotBuilder", robotBuilderVersion, "robotbuilder"),

                "toolchainVersion"     : new Tuple("Toolchain", toolchainVersion, "toolchain"),
        ]
    }
}
