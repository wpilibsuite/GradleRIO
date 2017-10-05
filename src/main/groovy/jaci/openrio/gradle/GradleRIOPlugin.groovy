package jaci.openrio.gradle

import jaci.gradle.EmbeddedTools
import jaci.openrio.gradle.frc.FRCPlugin
import jaci.openrio.gradle.wpi.WPIPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project;

class GradleRIOPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configurations.maybeCreate("nativeLib")
        project.configurations.maybeCreate("nativeZip")
        
        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(FRCPlugin)
        project.pluginManager.apply(WPIPlugin)
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerio")
    }
}