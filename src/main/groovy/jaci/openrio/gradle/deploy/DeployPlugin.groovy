package jaci.openrio.gradle.deploy

import groovy.transform.TupleConstructor
import jaci.gradle.EmbeddedTools
import jaci.gradle.deployers.*
import jaci.gradle.targets.TargetsSpec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Copy
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

class DeployPlugin implements Plugin<Project> {

    // Necessary to have access to project.configurations and such in the RuleSource
    @TupleConstructor
    class ProjectWrapper {
        Project project
    }

    void apply(Project project) {
        project.pluginManager.apply(EmbeddedTools)

        project.extensions.add('projectWrapper', new ProjectWrapper(project))

        def frc = new FRCExtension(project.container(RoboRIO), project.container(FRCJava), project.container(FRCNative))
        project.extensions.add('frc', frc)
    }

    static class DeployRules extends RuleSource {
        @Mutate
        void addRoboRios(TargetsSpec targets, final ExtensionContainer extensions) {
            def frcExt = extensions.getByName('frc')

            frcExt.roborio.each { roborio ->
                targets.create(roborio.name) { target ->
                    target.addresses << '172.22.11.2'
                    target.addresses << "roborio-${roborio.team}-frc.local".toString()
                    target.addresses << "10.${((int)roborio.team)/100}.${((int)roborio.team)%100}.2".toString()
                    target.asyncFind = true

                    target.user = 'admin'
                    target.password = ''
                    target.promptPassword = false

                    target.directory = '/home/lvuser'
                    target.mkdirs = true
                    target.timeout = 3

                    target.failOnMissing = true
                    if (roborio.remote != null) {
                        roborio.remote.resolveStrategy = DELEGATE_FIRST
                        roborio.remote.delegate = target
                        roborio.remote(target)
                    }
                }
            }
        }

        @Mutate
        void addDeployers(DeployersSpec deployers, final ExtensionContainer extensions) {
            FRCExtension frcExt = extensions.getByName('frc')
            Project project = extensions.getByName('projectWrapper').project

            ExportFileResourceTask netconsolehost_task = project.tasks.create('exportNetconsoleHost', ExportFileResourceTask) { task ->
                task.resource = "netconsole/netconsole-host"
                task.outfile = new File(project.buildDir, "gradlerio/resource/${task.resource}")
            }

            def nativeZips = project.configurations.nativeZip
            def zips = nativeZips.dependencies.findAll { dep -> dep != null && nativeZips.files(dep).size() > 0 }.collect { dep ->
                return [dep, project.tasks.create("unzipDependency${dep.name.capitalize()}", Copy) { task ->
                    task.from(project.zipTree(nativeZips.files(dep).first()))
                    task.into(new File(project.buildDir, "gradlerio/unzip/${dep.name}"))
                }]
            }

            def createBaseDeployer = { Deployer deployer, FRCDeployer ext ->
                deployer.predeploy << "whoami"
                // ARTIFACT: Netconsole
                deployer.artifacts.create("netconsolehost", FileArtifact) { artifact ->
                    artifact.predeploy << "killall -q netconsole-host 2> /dev/null || :"
                    artifact.file = netconsolehost_task.outputs.files.first()
                    artifact.directory = '/usr/local/frc/bin'
                    artifact.filename = 'netconsole-host'
                    artifact.postdeploy << 'chmod +x netconsole-host'
                }

                // Add netconsolehost export task dependency for this deployer
                project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { t -> t.dependsOn netconsolehost_task }

                // Add native libs (single .so)
                def nativeLibs = project.configurations.nativeLib
                nativeLibs.dependencies.findAll { dep -> dep != null && nativeLibs.files(dep).size() > 0 }.forEach { dep ->
                    def libfile = nativeLibs.files(dep).first()
                    // ARTIFACT: Native Libs
                    deployer.artifacts.create("nativeLib${dep.name.capitalize()}", FileArtifact) { artifact ->
                        artifact.file = libfile
                        artifact.directory = '/usr/local/frc/lib'
                    }
                }

                // Add native zips (from wpilib etc)
                zips.forEach { zipentry ->
                    def zipdep = zipentry.first()
                    def unziptask = zipentry.last()
                    if (unziptask.outputs.files.size() == 0) return;

                    def unzipdir = unziptask.outputs.files.first()
                    def nativelibs = [ "", "lib", "java/lib", "linux/athena" ].collectMany { dirext ->
                        def ft = project.fileTree(new File(unzipdir, dirext))
                        ft.include("*.so*")
                        ft.getFiles()
                    }

                    // ARTIFACT: Native Zips
                    deployer.artifacts.create("nativeZip${zipdep.name.capitalize()}", FileSetArtifact) { artifact ->
                        artifact.files = nativelibs
                        artifact.directory = '/usr/local/frc/lib'
                    }
                }

                // Add unzip tasks as dependencies to the main deploy task for this deployer
                project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                    zips.collect { z -> z[1] }.forEach { zt -> task.dependsOn zt }
                }

                deployer.postdeploy << "ldconfig" << "sync" << ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r || true"
                deployer.targets += ext.targets
            }

            frcExt.java.each { FRCJava java ->
                deployers.create(java.name) { deployer ->
                    createBaseDeployer(deployer, java)
                    // ARTIFACT: User Java
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
                        project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                            task.dependsOn robotCommandTask
                        }
                    }


                    if (java.deployer != null)
                        java.deployer.call(deployer)
                }
            }

            frcExt.nativ.each { FRCNative nativ ->
                deployers.create(nativ.name) { deployer ->
                    createBaseDeployer(deployer, nativ)
                    // TODO: Native
                }
            }
        }
    }
}