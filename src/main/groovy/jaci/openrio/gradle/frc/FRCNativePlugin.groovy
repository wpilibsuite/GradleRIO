package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.deployers.Deployer
import jaci.gradle.deployers.FileArtifact
import jaci.gradle.deployers.JavaArtifact
import jaci.gradle.deployers.NativeArtifact
import jaci.openrio.gradle.frc.ext.FRCNative
import org.gradle.api.Plugin
import org.gradle.api.Project

// TODO: Make @CompileStatic
class FRCNativePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) { }

    void configureDeployer(Project project, Deployer deployer, FRCNative frcNative) {
        NativeArtifact usercode = deployer.artifacts.create(frcNative.component, NativeArtifact) { artifact ->
            artifact.predeploy << ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null"
            artifact.predeploy << "rm -f ${frcNative.component} || true 2> /dev/null".toString()
            artifact.order = 100
            artifact.filename = frcNative.component
            artifact.platform = 'roborio'
            artifact.postdeploy << "chmod +x ${artifact.filename} && chown lvuser ${artifact.filename}".toString()
            artifact.postdeploy << "mkdir -p /usr/local/gradlerio/indexes/native" << "echo \$(pwd)/${artifact.filename} > /usr/local/gradlerio/indexes/native/${frcNative.name}.index".toString()
            artifact.postdeploy << "setcap 'cap_sys_nice=pe' ${artifact.filename} || true 2> /dev/null".toString()

            if (frcNative.artifact != null)
                frcNative.artifact.call(artifact)
        }

        // ARTIFACT: RobotCommand
        if (frcNative.robotCommand != null) {
            def indexcat = "\$(cat /usr/local/gradlerio/indexes/native/${frcNative.name}.index)".toString()
            def cmd = frcNative.robotCommand.call().toString().replace('<<BINARY>>', indexcat)
            cmd = [ "pushd \$(dirname ${indexcat})", cmd, "popd" ].join("\n")

            def cmdFile = new File(project.buildDir, "gradlerio/robotCommand")
            def robotCommandTask = project.tasks.create("robotCommand${frcNative.name.capitalize()}") { task ->
                task.doLast {
                    cmdFile.parentFile.mkdirs()
                    cmdFile.text = cmd
                }
            }

            deployer.artifacts.create("robotCommand", FileArtifact) { artifact ->
                artifact.file = cmdFile
                artifact.filename = "robotCommand"
                artifact.directory = "/home/lvuser"
                artifact.postdeploy << "chmod +x /home/lvuser/robotCommand && chown lvuser /home/lvuser/robotCommand"
            }

            // Add RobotCommand task
            project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                task.dependsOn robotCommandTask
            }
        }

        if (frcNative.deployer != null)
            frcNative.deployer.call(deployer)
    }
}
