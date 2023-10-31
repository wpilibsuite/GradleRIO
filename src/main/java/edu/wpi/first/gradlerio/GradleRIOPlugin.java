package edu.wpi.first.gradlerio;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.execution.MultipleBuildFailures;
import org.gradle.internal.resolve.ArtifactResolveException;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.util.internal.GUtil;

import edu.wpi.first.deployutils.DeployUtils;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.deployutils.deploy.target.discovery.TargetDiscoveryTask;
import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import edu.wpi.first.gradlerio.caching.WrapperInspector;
import edu.wpi.first.gradlerio.deploy.FRCDeployPlugin;
import edu.wpi.first.gradlerio.deploy.roborio.RoboRIO;
import edu.wpi.first.gradlerio.wpi.WPIPlugin;
import edu.wpi.first.gradlerio.OneDriveException;

public abstract class GradleRIOPlugin implements Plugin<Project> {

    public static abstract class BuildListener implements FlowAction<BuildListener.Parameters> {
        interface Parameters extends FlowParameters {
            @Input
            Property<BuildWorkResult> getBuildResult();
        }

        @Override
        public void execute(Parameters parameters) {
            Optional<Throwable> failure = parameters.getBuildResult().get().getFailure();
            if (failure.isPresent()) {
                try {
                    checkBuildFailed(failure.get());
                } catch (Throwable t) {
                    // Don't fail the build if we, for some reason, screw up
                    System.out.println(
                            "Error during build failure checking: " + t.getClass() + ": " + t.getMessage());
                }
            }
        }

    }

    @Inject
    protected abstract FlowScope getFlowScope();

    @Inject
    protected abstract FlowProviders getFlowProviders();

    @Override
    public void apply(Project project) {
        try {
            if (project.getRootDir().toString().toUpperCase().contains("ONEDRIVE")) {
                throw new OneDriveException(project);
            }
        } catch(OneDriveException e) {
            e.printError();
        }

        project.getPluginManager().apply(DeployUtils.class);
        project.getPluginManager().apply(FRCDeployPlugin.class);
        project.getPluginManager().apply(WPIPlugin.class);
        // project.getPluginManager().apply(ClionPlugin.class);
        // project.getPluginManager().apply(IDEPlugin.class);
        // project.getPluginManager().apply(TestPlugin.class);

        project.getTasks().withType(Wrapper.class).configureEach(wrapper -> {
            if (!project.hasProperty("no-gradlerio-wrapper")) {
                wrapper.setDistributionPath("permwrapper/dists");
                wrapper.setArchivePath("permwrapper/dists");
            }
        });

        disableCacheCleanup(project);

        project.getGradle().getTaskGraph().whenReady(graph -> {
            try {
                if (!project.hasProperty("skip-inspector")) {
                    inspector(project);
                }
            } catch (Exception e) {
                Logging.getLogger(this.getClass()).info("Inspector failed: " + e.getMessage());
            }
            ensureSingletons(project, graph);
        });

        getFlowScope().always(BuildListener.class, spec -> {
            spec.getParameters().getBuildResult().set(getFlowProviders().getBuildWorkResult());
        });
    }

    public static Action<Manifest> javaManifest(String robotMainClass) {
        return mf -> {
            mf.attributes(Map.of("Main-Class", robotMainClass));
        };
    }

    private static final String GRADLERIO_DISABLE_CACHE_CLEANUP_PROPERTY = "gradlerio.disable.cache.cleanup";
    private static final String CACHE_CLEANUP_PROPERTY = "org.gradle.cache.cleanup";

    private void disableCacheCleanup(Project project) {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("GR_CACHECLEANUP");
        Object cleanupProp = project.findProperty(GRADLERIO_DISABLE_CACHE_CLEANUP_PROPERTY);
        if (cleanupProp != null && cleanupProp.equals("false")) {
            logger.logErrorHead("Warning! You have the property gradlerio.disable.cache.cleanup set to false");
            logger.logError(
                    "This can cause issues when going to competition, since this means your dependencies can be deleted unwillingly after a month.");
            logger.logError("Remove this from your gradle.properties file unless you know what you're doing.");
            logger.logError("Note, this may result in you not being able to deploy code at competition");
            return;
        }

        try {
            File gradleProperties = new File(project.getGradle().getGradleUserHomeDir(), "gradle.properties");
            if (gradleProperties.isFile()) {
                Properties props = GUtil.loadProperties(gradleProperties);
                String cleanup = props.getProperty(CACHE_CLEANUP_PROPERTY);
                if (cleanup == null || !cleanup.equals("false")) {
                    logger.info("Disabling Gradle auto cache cleanup...");
                    props.setProperty(CACHE_CLEANUP_PROPERTY, "false");
                    logger.info("Saving gradle.properties");
                    GUtil.saveProperties(props, gradleProperties);
                    logger.info("Done!");
                }
            }
        } catch (Exception e) {
            logger.logError("Could not disable gradle cache cleanup. Run with --info for more information.");
            logger.info(e.getClass() + ": " + e.getMessage());
        }
    }

    private void inspector(Project project) {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("GR_INSPECTOR");
        project.allprojects(proj -> {
            if (!project.hasProperty("skip-inspector-" + WrapperInspector.NAME)) {
                logger.info("Running " + WrapperInspector.NAME + " inspector on project " + project.getPath());
                WrapperInspector.run(project, logger);
            }
        });
    }

    private void ensureSingletons(Project project, TaskExecutionGraph graph) {
        List<Task> allTasks = graph.getAllTasks();
        Set<String> visited = new HashSet<>();

        // Go in reverse - only use the latest version in the task graph (not earliest)
        for (int i = allTasks.size() - 1; i >= 0; i--) {
            Task t = allTasks.get(i);
            if (t instanceof SingletonTask) {
                String singletonName = ((SingletonTask) t).getSingletonName().get();
                if (visited.add(singletonName)) {
                    Logging.getLogger(this.getClass()).info("Singleton Task Using: " + t + " for " + singletonName);
                } else {
                    Logging.getLogger(this.getClass()).info("Singleton Task Disabling: " + t + " for " + singletonName);
                    t.setEnabled(false);
                }
            }
        }
    }

    private static void checkBuildFailed(Throwable result) {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("GR_BUILDFAILED_CHECKER");

        // Check if a we've failed due to a dependency error. If so, emit a warning that
        // you require an
        // internet connection.

        List<Throwable> rootExceptions = new ArrayList<>();
        Set<Throwable> exceptions = new HashSet<>();

        if (result instanceof MultipleBuildFailures) {
            rootExceptions.addAll(((MultipleBuildFailures) result).getCauses());
        } else {
            rootExceptions.add(result);
        }

        for (Throwable root : rootExceptions) {
            Throwable ex = root;
            while (ex != null) {
                exceptions.add(ex);
                logger.info("Build Exception: " + ex.getClass() + " -> " + ex.getMessage());
                ex = ex.getCause();
            }
        }

        // An array of string hashcodes makes sure we don't "overprint" errors.
        // def reasons = [] as Set<Integer>
        Set<Integer> reasons = new HashSet<>();

        for (Throwable t : exceptions) {
            if (t instanceof ArtifactResolveException) {
                if (reasons.add("ArtifactResolve".hashCode())) {
                    // Encourage user to run build task to prepare dependencies.
                    // ./gradlew deploy -PdeployDry will also work, but for safety we should
                    // encourage all downloads
                    // in case requirements change at competition.
                    logger.logErrorHead("Dependency Error!");
                    logger.logError("GradleRIO detected this build failed due to missing dependencies!");
                    logger.logError(
                            "Try again with `./gradlew build` whilst connected to the internet (not the robot!)");
                    logger.logError(
                            "If the error persists, ensure you are not behind a firewall / proxy server (common in schools)");
                }
            }
            if (t instanceof TaskExecutionException) {
                TaskExecutionException tee = (TaskExecutionException) t;
                Task task = tee.getTask();
                logger.info("Exception Task: " + task.getClass() + " -> " + task.getName());

                if (task instanceof TargetDiscoveryTask) {
                    RemoteTarget target = ((TargetDiscoveryTask) task).getTarget();
                    if (reasons.add(("Target" + target.getName()).hashCode())) {
                        logger.logErrorHead("Missing Target!");
                        if (target instanceof RoboRIO) {
                            logger.logErrorHead("=============================================");
                            logger.logErrorHead("Are you connected to the robot, and is it on?");
                            logger.logErrorHead("=============================================");
                        }
                        logger.logError("GradleRIO detected this build failed due to not being able to find \""
                                + target.getName() + "\"!");
                        logger.logError("Scroll up in this error log for more information.");
                    }
                } else if (task instanceof AbstractNativeCompileTask || task instanceof AbstractCompile) {
                    String reasonID = task.getName();
                    if (task instanceof AbstractNativeCompileTask) {
                        AbstractNativeCompileTask typedTask = (AbstractNativeCompileTask) task;
                        int indexOfPlatform = reasonID
                                .indexOf(StringGroovyMethods.capitalize(typedTask.getTargetPlatform().get().getName()));
                        reasonID = reasonID.substring(0, indexOfPlatform < 0 ? reasonID.length() : indexOfPlatform);
                        logger.info("ReasonID: " + reasonID);
                    }

                    if (reasons.add(("Compile" + reasonID).hashCode())) {
                        logger.logErrorHead("Compilation Error!");
                        logger.logError(
                                "GradleRIO detected this build failed due to a Compile Error (" + reasonID + ").");
                        logger.logError(
                                "Check that all your files are saved, then scroll up in this log for more information.");
                    }
                } else if (task instanceof AbstractLinkTask) {
                    String reasonID = task.getName();

                    AbstractLinkTask typedTask = (AbstractLinkTask) task;
                    int indexOfPlatform = reasonID
                            .indexOf(StringGroovyMethods.capitalize(typedTask.getTargetPlatform().get().getName()));
                    reasonID = reasonID.substring(0, indexOfPlatform < 0 ? reasonID.length() : indexOfPlatform);
                    logger.info("ReasonID: " + reasonID);

                    if (reasons.add(("Link" + reasonID).hashCode())) {
                        logger.logErrorHead("Linker Error!");
                        logger.logError(
                                "GradleRIO detected this build failed due to a Linker Error (" + reasonID + ").");
                        logger.logError(
                                "Check that all your files are saved, then scroll up in this log for more information.");
                    }
                }
            }
        }
    }
}
