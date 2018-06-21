package edu.wpi.first.gradlerio.frc

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.JavaArtifact
import org.gradle.api.Project

@CompileStatic
class FRCJavaArtifact extends JavaArtifact {
    FRCJavaArtifact(String name) {
        super(name)
        setJar('jar')

        predeploy << { DeployContext ctx ->
            ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null")
        }

        postdeploy << { DeployContext ctx ->
            def artifactName = filename ?: _jarfile.name
            ctx.execute("chmod +x /home/lvuser/robotCommand; chown lvuser /home/lvuser/robotCommand")
            ctx.execute("chmod +x ${artifactName}; chown lvuser ${artifactName}")
            ctx.execute("sync")
            ctx.execute(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r 2> /dev/null")
        }
    }

    List<String> jvmArgs = []
    List<String> arguments = []
    boolean debug = false
    int debugPort = 8348
    String debugFlags = "-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=${debugPort},server=y,suspend=y"

    def robotCommand = {
        "/usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmArgs.join(" ")} ${debug ? debugFlags : ""} -jar <<BINARY>> ${arguments.join(" ")}"
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
                def binFile = PathUtils.combine(ctx.workingDir(), filename ?: _jarfile.name)
                rCmd = rCmd.replace('<<BINARY>>', binFile)
                ctx.execute("echo '${rCmd}' > /home/lvuser/robotCommand")
            }
        }
        def conffile = new File(project.buildDir, "debug/${name}_${ctx.remoteTarget().name}.debugconfig")

        if (debug) {
            ctx.logger().log("====================================================================")
            ctx.logger().log("DEBUGGING ACTIVE ON PORT ${debugPort}!")
            ctx.logger().log("====================================================================")

            def target = ctx.selectedHost() + ":" + debugPort
            def dbcfg = [
                target: target,
                ipAddress: ctx.selectedHost(),
                port: debugPort
            ]

            def gbuilder = new GsonBuilder()
            gbuilder.setPrettyPrinting()
            conffile.text = gbuilder.create().toJson(dbcfg)
        } else {
            // Not debug, remove debug files
            if (conffile.exists()) conffile.delete()
        }
    }
}
