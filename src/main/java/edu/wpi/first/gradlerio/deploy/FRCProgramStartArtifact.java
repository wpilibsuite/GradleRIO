package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.embeddedtools.deploy.artifact.AbstractArtifact;
import edu.wpi.first.embeddedtools.deploy.context.DeployContext;

public class FRCProgramStartArtifact extends AbstractArtifact {

    @Inject
    public FRCProgramStartArtifact(String name, Project project) {
        super(name, project);

        this.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.ProgramStart);
    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.execute("sync");
        ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r 2> /dev/null");
    }

}
