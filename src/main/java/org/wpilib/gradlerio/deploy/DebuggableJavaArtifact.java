package org.wpilib.gradlerio.deploy;

import javax.inject.Inject;

import org.wpilib.deployutils.deploy.artifact.JavaArtifact;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.deployutils.deploy.sessions.IPSessionController;
import org.wpilib.deployutils.deploy.target.RemoteTarget;

public class DebuggableJavaArtifact extends JavaArtifact implements DebuggableArtifact {

    private int debugPort = 8349;

    public int getDebugPort() {
        return debugPort;
    }

    @Inject
    public DebuggableJavaArtifact(String name, RemoteTarget target) {
        super(name, target);
    }

    @Override
    public TargetDebugInfo getTargetDebugInfo() {
        DeployContext ctx = getTarget().getTargetDiscoveryTask().get().getActiveContext();

        if (ctx.getController() instanceof IPSessionController) {
            IPSessionController session = (IPSessionController)ctx.getController();
            return new JavaTargetDebugInfo(getName(), debugPort, session.getHost(), getTarget().getProject().getName());
        }
        return null;
    }



}
