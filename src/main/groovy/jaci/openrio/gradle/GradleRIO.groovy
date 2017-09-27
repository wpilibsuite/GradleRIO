package jaci.openrio.gradle

import org.gradle.api.*
import groovy.util.*

import jaci.openrio.gradle.deploy.DeployPlugin;
import jaci.openrio.gradle.wpi.WPIPlugin;

class GradleRIO implements Plugin<Project> {
    void apply(Project project) {
        project.configurations.maybeCreate("nativeLib")
        project.configurations.maybeCreate("nativeZip")
        
        project.pluginManager.apply('jaci.gradle.EmbeddedTools')
        project.pluginManager.apply(DeployPlugin)
        project.pluginManager.apply(WPIPlugin)
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerio")
    }
}