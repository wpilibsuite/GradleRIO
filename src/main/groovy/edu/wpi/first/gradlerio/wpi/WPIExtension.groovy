package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem

import javax.inject.Inject

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    String wpilibVersion = "2019.1.1-beta-2"
    String niLibrariesVersion = "2019.7.1"
    String opencvVersion = "3.4.3-19"

    String wpilibYear = '2019'

    String googleTestVersion = "1.8.0-4-4e4df22"

    String jreArtifactLocation = "edu.wpi.first.jdk:roborio-2019:11.0.1u13-1"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "2019.1.1-beta-1"
    String shuffleboardVersion = "2019.1.1-beta-2"
    String outlineViewerVersion = "2019.1.1-beta-1"
    String robotBuilderVersion = "2019.1.1-beta-2"
    String pathWeaverVersion = "2019.0.0-alpha-0"

    // WPILib Toolchain (https://github.com/wpilibsuite/toolchain-builder/releases/latest) version and tag
    String toolchainTag = 'v2019-3'
    String toolchainVersion = "2019-6.3.0"
    String toolchainVersionLow = "6.3"
    String toolchainVersionHigh = "6.3"

    WPIMavenExtension maven

    String frcYear = '2019'

    final Platforms platforms

    final Project project
    final String toolsClassifier

    @Inject
    WPIExtension(Project project) {
        this.project = project
        // Object factory breaks `wpi.maven {}`, hence instead we use extensions.create.
//        def factory = project.objects
//        maven = factory.newInstance(WPIMavenExtension, project)
        maven = ((ExtensionAware)this).extensions.create('maven', WPIMavenExtension, project)

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

        platforms = new Platforms()
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

    public static class Platforms {
        public static String desktop = desktopOS() + desktopArch(), roborio = "linuxathena"

        public static String desktopArch() {
            String arch = System.getProperty("os.arch")
            return (arch == "amd64" || arch == "x86_64") ? "x86-64" : "x86"
        }

        public static String desktopOS() {
            return OperatingSystem.current().isWindows() ? "windows" : OperatingSystem.current().isMacOsX() ? "osx" : "linux"
        }
    }
}
