package edu.wpi.first.gradlerio.deploy.roborio;

import edu.wpi.first.deployutils.deploy.artifact.FileArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;
import java.util.function.Function;
import javax.inject.Inject;

public class RobotCommandArtifact extends FileArtifact {

  private Function<DeployContext, String> startCommandFunc;

  @Inject
  public RobotCommandArtifact(String name, StagedDeployTarget target) {
    super(name, target);

    target.setDeployStage(this, DeployStage.FileDeploy);
  }

  public Function<DeployContext, String> getStartCommandFunc() {
    return startCommandFunc;
  }

  public void setStartCommandFunc(Function<DeployContext, String> startCommandFunc) {
    this.startCommandFunc = startCommandFunc;
  }

  @Override
  public void deploy(DeployContext ctx) {
    String content = startCommandFunc.apply(ctx);

    ctx.execute("echo '" + content + "' > /home/lvuser/robotCommand");
    ctx.execute("chmod +x /home/lvuser/robotCommand; chown lvuser /home/lvuser/robotCommand");
  }
}
