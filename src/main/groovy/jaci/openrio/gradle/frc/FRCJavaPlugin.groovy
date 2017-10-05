package jaci.openrio.gradle.frc

import jaci.gradle.deployers.Deployer
import jaci.gradle.deployers.FileArtifact
import jaci.gradle.deployers.JavaArtifact
import jaci.openrio.gradle.frc.ext.FRCExtension
import jaci.openrio.gradle.frc.ext.FRCJava
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class FRCJavaPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.afterEvaluate {
            project.tasks.withType(Jar).each { Jar task ->
                def frc = (FRCExtension) project.extensions.getByName("frc")
                frc.java.each { jconfig ->
                    if (jconfig.jarTask == task.name) {
                        if (jconfig.configureFatJar)
                            task.from project.configurations.compile.collect {
                                it.isDirectory() ? it : project.zipTree(it)
                            }

                        if (jconfig.addManifest) {
                            def mfest = { mf ->
                                mf.attributes 'Main-Class': 'edu.wpi.first.wpilibj.RobotBase'
                                mf.attributes 'Robot-Class': jconfig.robotMainClass
                                jconfig.extraManifest.forEach { c -> mf.with(c) }
                            }
                            task.manifest(mfest)
                        }
                    }
                }
            }
        }
    }

    void configureDeployer(Project project, Deployer deployer, FRCJava java) {
        JavaArtifact usercode = deployer.artifacts.create(java.jarTask, JavaArtifact) { artifact ->
            artifact.predeploy << ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null"
            artifact.order = 100
            artifact.filename = 'FRCUserProgram.jar'
            artifact.postdeploy << "chmod +x ${artifact.filename} && chown lvuser ${artifact.filename}".toString()
            artifact.postdeploy << "mkdir -p /usr/local/gradlerio/indexes/java" << "echo \$(pwd)/${artifact.filename} > /usr/local/gradlerio/indexes/java/${java.name}.index".toString()

            if (java.artifact != null)
                java.artifact.call(artifact)
        }

        // ARTIFACT: RobotCommand
        if (java.robotCommand != null) {
            def cmd = java.robotCommand.call().toString().replace('<<BINARY>>', "\$(cat /usr/local/gradlerio/indexes/java/${java.name}.index)".toString())
            def cmdFile = new File(project.buildDir, "gradlerio/robotCommand")
            def robotCommandTask = project.tasks.create("robotCommand${java.name.capitalize()}") { task ->
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
            project.tasks.matching { t -> t.name == "frc${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                task.dependsOn robotCommandTask
            }
        }


        if (java.deployer != null)
            java.deployer.call(deployer)
    }
}
