package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

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
            def gdbfile = new File(project.buildDir, "debug/${name}.gdbcommands")
            def isWin = OperatingSystem.current().isWindows()
            def cmdfile = new File(project.buildDir, "debug/${name}${isWin ? ".bat" : ""}")

            ctx.logger().log("====================================================================")
            ctx.logger().log("DEBUGGING ACTIVE ON PORT ${debugPort}!")
            ctx.logger().log("Launch debugger with ${isWin ? "" : "./"}${Paths.get(project.rootDir.toURI()).relativize(Paths.get(cmdfile.toURI())).toString()}")
            ctx.logger().log("manually in case of conflicts.")
            ctx.logger().log("====================================================================")

            def init_commands = [
                'set gnutarget elf32-littlearm',
                "file ${_nativefile.absolutePath.replaceAll("\\\\", "/")}".toString(),
                "target remote ${ctx.selectedHost()}:${debugPort}".toString()
            ]

            def gdbpath = new File(WPIToolchainPlugin.toolchainInstallDirectory(), "bin/arm-frc-linux-gnueabi-gdb").absolutePath
            def cmdline = [
                "${isWin ? "" : "./"}${gdbpath}${isWin ? ".exe" : ""}", "-ix=\"${gdbfile.absolutePath.replaceAll("\\\\", "/")}\""
            ]

            gdbfile.parentFile.mkdirs()
            gdbfile.text = init_commands.join('\n')
            cmdfile.text = cmdline.join(" ")
        }
    }
}
