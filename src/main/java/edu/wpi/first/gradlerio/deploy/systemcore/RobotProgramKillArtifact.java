package edu.wpi.first.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;

public class RobotProgramKillArtifact extends AbstractArtifact {

    @Inject
    public RobotProgramKillArtifact(String name, StagedDeployTarget target) {
        super(name, target);

        target.setDeployStage(this, DeployStage.ProgramKill);
    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.execute("sudo systemctl stop robot 2> /dev/null");
    }
}
