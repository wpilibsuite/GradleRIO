package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import jaci.openrio.gradle.wpi.toolchain.install.LinuxToolchainInstaller
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.nativeplatform.HeaderExportingSourceSet
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.process.ExecSpec

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

    NativeBinarySpec _bin

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
            ctx.logger().log("====================================================================")

            def srcpaths = []
            _bin.inputs.withType(HeaderExportingSourceSet) { HeaderExportingSourceSet ss ->
                srcpaths += ss.source.srcDirs
                srcpaths += ss.exportedHeaders.srcDirs
            }
            _bin.libs.each { NativeDependencySet ds ->
                srcpaths += ds.includeRoots
            }

            def init_commands = [
                'set gnutarget elf32-littlearm',
                "file ${_nativefile.absolutePath.replaceAll("\\\\", "/")}".toString(),
                "target remote ${ctx.selectedHost()}:${debugPort}".toString()
            ] as List<String>

            if (!(WPIToolchainPlugin.getActiveInstaller() instanceof LinuxToolchainInstaller))
                init_commands += ["set sysroot \"${WPIToolchainPlugin.toolchainInstallDirectory().absolutePath}\""] as List<String>
            init_commands += srcpaths.collect { "dir \"${it}\"" } as List<String>

            def gdbpath = new File(WPIToolchainPlugin.toolchainInstallDirectory(), "bin/arm-frc-linux-gnueabi-gdb").absolutePath
            def cmdline = [
                "${gdbpath}${isWin ? ".exe" : ""}", "-ix=\"${gdbfile.absolutePath.replaceAll("\\\\", "/")}\""
            ]

            gdbfile.parentFile.mkdirs()
            gdbfile.text = init_commands.join('\n')
            cmdfile.text = cmdline.join(" ")

            if (OperatingSystem.current().isUnix()) {
                project.exec { ExecSpec spec ->
                    spec.commandLine "chmod"
                    spec.args("0755", cmdfile.absolutePath)
                }
            }
        }
    }
}
