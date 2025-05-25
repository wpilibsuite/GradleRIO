package edu.wpi.first.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;

public class RobotProgramStartArtifact extends AbstractArtifact {

    @Inject
    public RobotProgramStartArtifact(String name, StagedDeployTarget target) {
        super(name, target);

        target.setDeployStage(this, DeployStage.ProgramStart);
    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.execute("sudo systemctl enable robot 2> /dev/null");
        ctx.execute("sudo systemctl start robot 2> /dev/null");
        ctx.execute("sudo sync");
    }
}
