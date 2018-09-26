package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.test.sim.SimulationPlugin
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.test.googletest.plugins.GoogleTestPlugin

@CompileStatic
class TestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(SimulationPlugin)

        project.pluginManager.apply(JavaTestPlugin)

        project.plugins.withType(GoogleTestPlugin).whenPluginAdded {
            project.pluginManager.apply(NativeTestPlugin)
        }
    }

    static String envDelimiter() {
        return OperatingSystem.current().isWindows() ? ";" : ":"
    }

}
