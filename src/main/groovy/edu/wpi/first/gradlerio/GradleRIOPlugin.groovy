package edu.wpi.first.gradlerio

import edu.wpi.first.gradlerio.caching.WrapperInspector
import edu.wpi.first.gradlerio.frc.FRCPlugin
import edu.wpi.first.gradlerio.ide.ClionPlugin
import edu.wpi.first.gradlerio.ide.IDEPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import edu.wpi.first.gradlerio.wpi.WPIPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.log.ETLoggerFactory
import org.apache.log4j.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.tasks.wrapper.Wrapper

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

        project.tasks.withType(Wrapper).configureEach { Wrapper wrapper ->
            if (!project.hasProperty('no-gradlerio-wrapper')) {
                wrapper.setDistributionPath('permwrapper/dists')
                wrapper.setArchivePath('permwrapper/dists')
            }
        }

        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            try {
                if (!project.hasProperty("skip-inspector"))
                    inspector(project)
            } catch (Exception e) {
                Logger.getLogger(this.class).info("Inspector failed: ${e.message}")
            }
            ensureSingletons(project, graph)
        }
    }

    void inspector(Project project) {
        def logger = ETLoggerFactory.INSTANCE.create("GR_INSPECTOR")
        project.allprojects.each { Project proj ->
            if (!project.hasProperty("skip-inspector-${WrapperInspector.NAME}")) {
                logger.info("Running ${WrapperInspector.NAME} inspector on project ${project.path}")
                WrapperInspector.run(project, logger)
            }
        }
    }

    void ensureSingletons(Project project, TaskExecutionGraph graph) {
        Map<String, Task> singletonMap = [:]
        graph.getAllTasks().each { Task t ->
            if (t instanceof SingletonTask) {
                String singletonName = (t as SingletonTask).singletonName()
                if (singletonMap.containsKey(singletonName)) {
                    Logger.getLogger(this.class).info("Singleton task on graph, disabling: ${t} for ${singletonName}")
                    t.setEnabled(false)
                } else {
                    Logger.getLogger(this.class).info("Singleton task on graph, using: ${t} for ${singletonName}")
                    singletonMap.put(singletonName, t)
                }
            }
        }
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