package edu.wpi.first.gradlerio.deploy;

import edu.wpi.first.deployutils.deploy.artifact.JavaArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.sessions.IPSessionController;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import javax.inject.Inject;

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
      IPSessionController session = (IPSessionController) ctx.getController();
      return new JavaTargetDebugInfo(
          getName(), debugPort, session.getHost(), getTarget().getProject().getName());
    }
    return null;
  }
}
