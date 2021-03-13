package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.NativeExecutableArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.sessions.IPSessionController;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public class DebuggableNativeArtifact extends NativeExecutableArtifact implements DebuggableArtifact {

    private int debugPort = 8349;

    public int getDebugPort() {
        return debugPort;
    }

    @Inject
    public DebuggableNativeArtifact(String name, RemoteTarget target) {
        super(name, target);
    }

    @Override
    public TargetDebugInfo getTargetDebugInfo() {
        DeployContext ctx = getTarget().getTargetDiscoveryTask().get().getActiveContext();

        if (ctx.getController() instanceof IPSessionController) {
            IPSessionController session = (IPSessionController)ctx.getController();
            return new NativeTargetDebugInfo(debugPort, session.getHost());
        }
        return null;
    }

}
