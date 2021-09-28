package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.deployutils.deploy.DeployExtension;

public abstract class WPIRemoteTarget extends StagedDeployTarget {

    private final Property<Boolean> debug;

    /**
     * Gets or Sets if debugging should be enabled for this target.
     *
     * Implies debug binaries in C++. For Java, use `wpi.java.debugJni = true`
     * to deploy debug JNI artifacts.
     *
     * @return Property for debug mode.
     */
    public Property<Boolean> getDebug() {
        return debug;
    }

    private final Provider<String> buildType;

    /**
     * Gets a mapping of getDebug() to the correct build type for C++.
     *
     * @return Build Type mapping for getDebug()
     */
    public Provider<String> getBuildType() {
        return buildType;
    }

    public final TaskProvider<TargetDebugFileTask> debugFileTask;

    public TaskProvider<TargetDebugFileTask> getDebugFileTask() {
        return debugFileTask;
    }

    @Inject
    public WPIRemoteTarget(String name, Project project, DeployExtension de) {
        super(name, project, de);

        debug = project.getObjects().property(Boolean.class);
        debug.set(false);

        buildType = debug.map(x -> x ? "debug" : "release");

        debugFileTask = project.getTasks().register("writeDebugInfo" + name, TargetDebugFileTask.class, t -> {
            t.getDebugFile().set(project.getLayout().getBuildDirectory().file("debug/" + name + ".json"));
            t.setTarget(this);
            t.dependsOn(getTargetDiscoveryTask());
        });
        getDeployTask().configure(t -> t.dependsOn(debugFileTask));
    }

}
