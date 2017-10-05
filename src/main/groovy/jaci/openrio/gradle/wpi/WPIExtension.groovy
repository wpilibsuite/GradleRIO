package jaci.openrio.gradle.wpi

import org.gradle.api.Project

class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven/release) libs
    String wpilibVersion = "+"
    String ntcoreVersion = "+"
    String opencvVersion = "+"
    String cscoreVersion = "+"

    // WPILib C++ Only libs (Note: HAL assumed same version as wpilib)
    String wpiutilVersion = "+"

    // Third Party (dev.imjac.in/maven/thirdparty) libs
    String ctreVersion = "+"
    String navxVersion = "+"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "+"
    String javaInstallerVersion = "+"

    WPIExtension(Project project) { }

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
                "navxVersion" : new Tuple("NavX", navxVersion, "navx"),

                "smartDashboardVersion" : new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
                "javaInstallerVersion" : new Tuple("JavaInstaller", javaInstallerVersion, "javainstaller")
        ]
    }
}