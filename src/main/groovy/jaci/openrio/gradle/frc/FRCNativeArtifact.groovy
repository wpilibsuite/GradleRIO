package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.NativeArtifact
import org.gradle.api.Project

@CompileStatic
class FRCNativeArtifact extends NativeArtifact {
    FRCNativeArtifact(String name) {
        super(name)
        targetPlatform = 'roborio'

        predeploy << { DeployContext ctx ->
            ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null")
        }

        postdeploy << { DeployContext ctx ->
            def artifactName = filename ?: _nativefile.name
            ctx.execute("chmod +x /home/lvuser/robotCommand; chown lvuser /home/lvuser/robotCommand")
            ctx.execute("chmod +x ${artifactName}; chown lvuser ${artifactName}")
            ctx.execute("sync")
            ctx.execute("ldconfig")
            ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r 2> /dev/null")
        }
    }

    List<String> arguments = []

    Object robotCommand = {
        "/usr/local/frc/bin/netconsole-host <<BINARY>> ${arguments.join(" ")}"
    }

    @Override
    void deploy(Project project, DeployContext ctx) {
        super.deploy(project, ctx)

        if (robotCommand) {
            String rCmd = null
            if (robotCommand instanceof Closure)
                rCmd = (robotCommand as Closure).call(this).toString()
            else if (robotCommand instanceof String)
                rCmd = (robotCommand as String)

            if (rCmd != null) {
                def binFile = PathUtils.combine(ctx.workingDir(), filename ?: _nativefile.name)
                rCmd = rCmd.replace('<<BINARY>>', binFile)
                ctx.execute("echo '${rCmd}' > /home/lvuser/robotCommand")
            }
        }
    }
}
