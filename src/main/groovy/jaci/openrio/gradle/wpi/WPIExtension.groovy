package jaci.openrio.gradle.wpi

import org.gradle.api.Project

class WPIExtension {
    String wpilibVersion = "+"
    String ntcoreVersion = "+"
    String opencvVersion = "+"
    String cscoreVersion = "+"

    String ctreVersion = "+"
    String navxVersion = "+"

    WPIExtension(Project project) { }
}