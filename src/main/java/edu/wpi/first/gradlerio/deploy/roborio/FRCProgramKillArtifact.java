package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.EncodingGroovyMethods;

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


        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            throw new RuntimeException(e1);
        }

        try (DigestInputStream it = new DigestInputStream(ToolInstallTask.class.getResourceAsStream("/frcKillRobot.sh"), md);
            ByteArrayOutputStreamAccessor dump = new ByteArrayOutputStreamAccessor()) {
            it.transferTo(dump);
            String local = EncodingGroovyMethods.encodeHex(md.digest()).toString();

            String result = ctx.execute("md5sum /usr/local/frc/bin/frcKillRobot.sh").getResult();
            if (result != null && result.toLowerCase().startsWith(local.toLowerCase())) {
                ctx.getLogger().log("Skipping redeploy of frcKillRobot script");
            } else {
                ctx.getLogger().log("Redeploying frcKillRobot script");
                try (InputStream out = new ByteArrayInputStream(dump.getBackingArray(), 0, dump.getBackingLength())) {
                    ctx.put(out, "/usr/local/frc/bin/frcKillRobot.sh.tmp");
                }
                ctx.execute("mv /usr/local/frc/bin/frcKillRobot.sh.tmp /usr/local/frc/bin/frcKillRobot.sh && chmod +x /usr/local/frc/bin/frcKillRobot.sh");
                ctx.execute("sync");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null");
    }
}
