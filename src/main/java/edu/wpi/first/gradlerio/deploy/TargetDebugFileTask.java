package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public class TargetDebugFileTask extends DefaultTask {

    private RemoteTarget target;

    @Internal
    public RemoteTarget getTarget() {
        return target;
    }

    public void setTarget(RemoteTarget target) {
        this.target = target;
    }

    @Inject
    public TargetDebugFileTask() {
        getOutputs().upToDateWhen((spec) -> false);
    }

    @TaskAction
    public void execute() {
        for (DebuggableJavaArtifact java : target.getArtifacts().withType(DebuggableJavaArtifact.class)) {

        }
    }
}
