package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.NativeArtifact
import org.gradle.api.Project

import java.nio.file.Paths

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
    boolean debug = false
    int debugPort = 8348

    def robotCommand = { DeployContext ctx, FRCNativeArtifact self ->
        "${self.debug ? "gdbserver host:${self.debugPort}" : ''} <<BINARY>> ${self.arguments.join(" ")}"
    }

    @Override
    void deploy(Project project, DeployContext ctx) {
        super.deploy(project, ctx)

        if (robotCommand) {
            String rCmd = null
            if (robotCommand instanceof Closure)
                rCmd = (robotCommand as Closure).call([ctx, this]).toString()
            else if (robotCommand instanceof String)
                rCmd = (robotCommand as String)

            if (rCmd != null) {
                def binFile = PathUtils.combine(ctx.workingDir(), filename ?: _nativefile.name)
                rCmd = rCmd.replace('<<BINARY>>', binFile)
                ctx.execute("echo '${rCmd}' > /home/lvuser/robotCommand")
            }
        }

        if (debug) {
            def outfile = new File(project.buildDir, "debug/${name}.gdbcommands")

            ctx.logger().log("====================================================================")
            ctx.logger().log("DEBUGGING ACTIVE ON PORT ${debugPort}!")
            ctx.logger().log("Launch debugger with gdb -ix='${Paths.get(project.rootDir.toURI()).relativize(Paths.get(outfile.toURI()))}'")
            ctx.logger().log("NOTE: If you are running GDB on multiple targets, you will have to run")
            ctx.logger().log("      (gdb) target remote host:${debugPort}")
            ctx.logger().log("manually in case of conflicts.")
            ctx.logger().log("====================================================================")

            def init_commands = [
                'set gnutarget elf32-littlearm',
                "file ${_nativefile.absolutePath}".toString(),
                "target remote ${ctx.selectedHost()}:${debugPort}".toString()
            ]

            outfile.parentFile.mkdirs()
            outfile.text = init_commands.join('\n')
        }
    }
}
