package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;

public class FRCProgramStartArtifact extends AbstractArtifact {

    @Inject
    public FRCProgramStartArtifact(String name, StagedDeployTarget target) {
        super(name, target);

        target.setDeployStage(this, DeployStage.ProgramStart);
    }

    @Override
    public void deploy(DeployContext ctx) {
        ctx.execute("sync");
        String result = ctx.execute("/usr/local/natinst/bin/nirtcfg --file=/etc/natinst/share/ni-rt.ini --get section=systemsettings,token=NoApp.enabled,value=unknown").getResult();
        if (result != null && result.trim().equalsIgnoreCase("true")) {
            ctx.getLogger().logError("NoApp is set on the device. Robot program cannot be started. Disable NoApp either with the imaging tool or by holding the User button for 5 seconds");
        } else {
            ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r 2> /dev/null");
        }
    }

}
