package org.wpilib.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.wpilib.deployutils.deploy.artifact.AbstractArtifact;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.gradlerio.deploy.DeployStage;
import org.wpilib.gradlerio.deploy.StagedDeployTarget;

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
