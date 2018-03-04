package jaci.openrio.gradle

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.openrio.gradle.frc.FRCPlugin
import jaci.openrio.gradle.ide.ClionPlugin
import jaci.openrio.gradle.ide.EditorConfigurationTask
import jaci.openrio.gradle.ide.IDEPlugin
import jaci.openrio.gradle.sim.SimulationPlugin
import jaci.openrio.gradle.telemetry.TelemetryPlugin
import jaci.openrio.gradle.wpi.WPIPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.java.archives.internal.DefaultManifest

@CompileStatic
class GradleRIOPlugin implements Plugin<Project> {
    // Necessary to have access to project.configurations and such in the RuleSource
    class ProjectWrapper {
        Project project

        ProjectWrapper(Project project) { this.project = project }
    }

    void apply(Project project) {
        // These configurations only act for the JAVA portion of GradleRIO
        // Native libraries have their own dependency management system
        project.configurations.maybeCreate("nativeLib")
        project.configurations.maybeCreate("nativeZip")

        project.configurations.maybeCreate("nativeSimulationLib")
        project.configurations.maybeCreate("nativeSimulationZip")

        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(FRCPlugin)
        project.pluginManager.apply(WPIPlugin)
        project.pluginManager.apply(ClionPlugin)
        project.pluginManager.apply(IDEPlugin)
        project.pluginManager.apply(TelemetryPlugin)
        project.pluginManager.apply(SimulationPlugin)

        project.extensions.add('projectWrapper', new ProjectWrapper(project))
    }

    static Closure javaManifest(String robotClass) {
        return { DefaultManifest mf ->
            mf.attributes 'Main-Class': 'edu.wpi.first.wpilibj.RobotBase'
            mf.attributes 'Robot-Class': robotClass
        }
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerio")
    }
}