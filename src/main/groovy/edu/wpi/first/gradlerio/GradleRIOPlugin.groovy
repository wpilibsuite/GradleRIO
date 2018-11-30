package edu.wpi.first.gradlerio

import edu.wpi.first.gradlerio.caching.WrapperInspector
import edu.wpi.first.gradlerio.frc.FRCPlugin
import edu.wpi.first.gradlerio.ide.ClionPlugin
import edu.wpi.first.gradlerio.ide.IDEPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import edu.wpi.first.gradlerio.wpi.WPIPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import jaci.gradle.log.ETLoggerFactory
import org.apache.log4j.Logger
import org.gradle.BuildResult
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.execution.MultipleBuildFailures
import org.gradle.internal.resolve.ArtifactResolveException
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask

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

        project.tasks.register("downloadAll", DownloadAllTask, { DownloadAllTask t ->
            t.group = "GradleRIO"
            t.description = "Download all dependencies that may be used by this project"
        } as Action<DownloadAllTask>)

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

        project.gradle.buildFinished { BuildResult result ->
            if (result.failure != null) {
                try {
                    checkBuildFailed(project, result)
                } catch (Throwable t) {
                    // Don't fail the build if we, for some reason, screw up
                    println("Error during build failure checking: ${t.getClass()} ${t.getMessage()}")
                }
            }
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

    void checkBuildFailed(Project project, BuildResult result) {
        def logger = ETLoggerFactory.INSTANCE.create("GR_BUILDFAILED_CHECKER")

        // Check if a we've failed due to a dependency error. If so, emit a warning that you require an
        // internet connection.

        def rootExceptions = [] as List<? extends Throwable>
        def exceptions = [] as List<? extends Throwable>

        if (result.failure instanceof MultipleBuildFailures) {
            ((MultipleBuildFailures)result.failure).causes.each { Throwable t ->
                rootExceptions.add(t)
            }
        } else {
            rootExceptions.add(result.failure)
        }

        rootExceptions.each { Throwable root ->
            def ex = root
            while (ex != null) {
                if (!exceptions.contains(ex))
                    exceptions.add(ex)
                logger.info("Build Exception: ${ex.getClass()} -> ${ex.getMessage()}")
                ex = ex.cause
            }
        }

        exceptions.each { Throwable t ->
            if (t instanceof ArtifactResolveException) {
                // Encourage user to run downloadAll task to prepare dependencies.
                // ./gradlew deploy -PdeployDry will also work, but for safety we should encourage all downloads
                // in case requirements change at competition.
                logger.logError("GradleRIO detected this build failed due to missing dependencies!")
                logger.logError("Try again with `./gradlew downloadAll` whilst connected to the internet (not the robot!)")
                logger.logError("If the error persists, ensure you are not behind a firewall / proxy server (common in schools)")
            }

            if (t instanceof TaskExecutionException) {
                TaskExecutionException tee = (TaskExecutionException) t
                def task = tee.task
                logger.info("Exception Task: ${task.getClass()} -> ${task.getName()}")

                if (task instanceof TargetDiscoveryTask) {
                    def target = ((TargetDiscoveryTask)task).target
                    logger.logError("GradleRIO detected this build failed due to not being able to find \"${target.name}\"!")
                    logger.logError("Scroll up in this error log for more information.")
                } else if (task instanceof AbstractNativeCompileTask || task instanceof AbstractCompile) {
                    logger.logError("GradleRIO detected this build failed due to a Compile Error (${task.getName()}).")
                    logger.logError("Check that all your files are saved, then scroll up in this log for more information.")
                }
            }
        }
    }

    static Closure javaManifest(String robotMainClass) {
        return { DefaultManifest mf ->
            mf.attributes 'Main-Class': robotMainClass
        }
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerio")
    }
}
