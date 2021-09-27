package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.gradlerio.deploy.DeployStage;

public class FRCProgramStartArtifact extends AbstractArtifact {

    @Inject
    public FRCProgramStartArtifact(String name, RemoteTarget target) {
        super(name, target);

        this.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.ProgramStart);
    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.execute("sync");
        ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r 2> /dev/null");
    }

}
