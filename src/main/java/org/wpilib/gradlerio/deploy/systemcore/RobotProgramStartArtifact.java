package org.wpilib.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.wpilib.deployutils.deploy.artifact.AbstractArtifact;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.gradlerio.deploy.DeployStage;
import org.wpilib.gradlerio.deploy.StagedDeployTarget;

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
