package jaci.openrio.gradle

import groovy.transform.TupleConstructor
import jaci.gradle.EmbeddedTools
import jaci.openrio.gradle.frc.FRCPlugin
import jaci.openrio.gradle.wpi.WPIPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project;

class GradleRIOPlugin implements Plugin<Project> {
    // Necessary to have access to project.configurations and such in the RuleSource
    @TupleConstructor
    class ProjectWrapper {
        Project project
    }

    void apply(Project project) {
        project.configurations.maybeCreate("nativeLib")
        project.configurations.maybeCreate("nativeZip")
        
        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(FRCPlugin)
        project.pluginManager.apply(WPIPlugin)

        project.extensions.add('projectWrapper', new ProjectWrapper(project))
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerio")
    }
}