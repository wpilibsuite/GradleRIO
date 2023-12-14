package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;
import edu.wpi.first.gradlerio.wpi.dependencies.tools.ToolInstallTask;

public class FRCProgramKillArtifact extends AbstractArtifact {

    @Inject
    public FRCProgramKillArtifact(String name, StagedDeployTarget target) {
        super(name, target);

        target.setDeployStage(this, DeployStage.ProgramKill);


    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.getLogger().log("  Deploying new frcKillRobot script");
        try (InputStream it = ToolInstallTask.class.getResourceAsStream("/frcKillRobot.sh")) {
            ctx.put(it, "/usr/local/frc/bin/frcKillRobot.sh.tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ctx.execute("mv /usr/local/frc/bin/frcKillRobot.sh.tmp /usr/local/frc/bin/frcKillRobot.sh && chmod +x /usr/local/frc/bin/frcKillRobot.sh");
        ctx.execute("sync");
        ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null");
    }
}
