package jaci.openrio.gradle.wpi

import org.gradle.api.Project

class WPIExtension {
    String wpilibMaven = "release"

    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    String wpilibVersion = "+"
    String ntcoreVersion = "+"
    String opencvVersion = "+"
    String cscoreVersion = "+"

    // Third Party (dev.imjac.in/maven/thirdparty) libs
    String ctreVersion = "+"
    String navxVersion = "+"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "+"
    String javaInstallerVersion = "+"

    WPIExtension(Project project) { }

    Map<String, Tuple> versions() {
        return [
                "wpilibMaven" : new Tuple("WPI Maven", wpilibMaven),

                "wpilibVersion" : new Tuple("WPILib", wpilibVersion),
                "ntcoreVersion" : new Tuple("NTCore", ntcoreVersion),
                "opencvVersion" : new Tuple("OpenCV", opencvVersion),
                "cscoreVersion" : new Tuple("CSCore", cscoreVersion),

                "ctreVersion" : new Tuple("CTRE", ctreVersion),
                "navxVersion" : new Tuple("NavX", navxVersion),

                "smartDashboardVersion" : new Tuple("SmartDashboard", smartDashboardVersion),
                "javaInstallerVersion" : new Tuple("JavaInstaller", javaInstallerVersion)
        ]
    }
}