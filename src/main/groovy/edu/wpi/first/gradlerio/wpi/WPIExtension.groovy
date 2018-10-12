package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    String wpilibVersion = "2018.4.1-20180921151738-1194-gf89274f"
    String niLibrariesVersion = "2018.17.1"
    String opencvVersion = "3.2.0"

    String wpilibYear = '2019'

    String googleTestVersion = "1.8.0-1-4e4df22"

    String jreArtifactLocation = "edu.wpi.first.jdk:roborio-2019:11.0.0u28-1"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "3.0.0"
    String shuffleboardVersion = "1.424242.3.1-20181007163852-59-gc57db86"
    String outlineViewerVersion = "2.424242.0.4-20181007161343-8-g71b005c"
    String robotBuilderVersion = "3.0.1"
    String pathWeaverVersion = "v0.0.0"

    // WPILib Toolchain (https://github.com/wpilibsuite/toolchain-builder/releases/latest) version and tag
    String toolchainTag = 'v2019-2'
    String toolchainVersion = "2019-6.3.0"
    String toolchainVersionLow = "6.3"
    String toolchainVersionHigh = "6.3"

    WPIMavenExtension maven

    String frcYear = '2019'

    final Project project
    final String nativeClassifier
    final String toolsClassifier

    WPIExtension(Project project) {
        this.project = project
        maven = ((ExtensionAware)this).extensions.create('maven', WPIMavenExtension, project)

        if (project.hasProperty('forceNativeClassifier')) {
            this.nativeClassifier = project.findProperty('forceNativeClassifier')
        } else {
            this.nativeClassifier = (
                OperatingSystem.current().isWindows() ?
                        System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                        OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                        null
            )
        }

        if (project.hasProperty('forceToolsClassifier')) {
            this.toolsClassifier = project.findProperty('forceToolsClassifier')
        } else {
            this.toolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch") == 'amd64' ? 'win64' : 'win32' :
                            OperatingSystem.current().isMacOsX() ? "mac64" :
                                    OperatingSystem.current().isLinux() ? "linux64" :
                                            null
            )
        }
    }

    private String frcHomeCache

    String getFrcHome() {
        if (frcHomeCache != null) {
            return this.frcHomeCache
        }
        String frcHome = ''
        if (OperatingSystem.current().isWindows()) {
            String publicFolder = System.getenv('PUBLIC')
            if (publicFolder == null) {
                publicFolder = "C:\\Users\\Public"
            }
            frcHome = new File(publicFolder, "frc${this.frcYear}").toString()
        } else {
            def userFolder = System.getProperty("user.home")
            frcHome = new File(userFolder, "frc${this.frcYear}").toString()
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
