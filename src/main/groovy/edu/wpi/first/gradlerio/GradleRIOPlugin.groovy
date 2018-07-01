package edu.wpi.first.gradlerio

import edu.wpi.first.gradlerio.frc.FRCPlugin
import edu.wpi.first.gradlerio.ide.ClionPlugin
import edu.wpi.first.gradlerio.ide.IDEPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import edu.wpi.first.gradlerio.wpi.WPIPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
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

        project.configurations.maybeCreate("nativeDesktopLib")
        project.configurations.maybeCreate("nativeDesktopZip")

        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(FRCPlugin)
        project.pluginManager.apply(WPIPlugin)
        project.pluginManager.apply(ClionPlugin)
        project.pluginManager.apply(IDEPlugin)
        project.pluginManager.apply(TestPlugin)

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