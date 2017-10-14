package jaci.openrio.gradle.frc

import groovy.transform.TupleConstructor
import jaci.gradle.EmbeddedTools
import jaci.gradle.deployers.CacheMethod
import jaci.gradle.deployers.Deployer
import jaci.gradle.deployers.DeployersSpec
import jaci.gradle.deployers.FileArtifact
import jaci.gradle.deployers.FileSetArtifact
import jaci.gradle.targets.TargetsSpec
import jaci.openrio.gradle.frc.ext.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.internal.changedetection.state.ZipTree
import org.gradle.api.internal.file.collections.DefaultConfigurableFileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Copy
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

class FRCPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.pluginManager.apply(EmbeddedTools)

        def frc = new FRCExtension(project.container(RoboRIO), project.container(FRCJava), project.container(FRCNative))
        project.extensions.add('frc', frc)

        project.pluginManager.apply(FRCJavaPlugin)
        project.pluginManager.apply(FRCNativePlugin)
    }

    static class DeployRules extends RuleSource {
        @Mutate
        void addRoboRios(TargetsSpec targets, final ExtensionContainer extensions) {
            def frcExt = extensions.getByName('frc')

            frcExt.roborio.each { roborio ->
                targets.create(roborio.name) { target ->
                    target.addresses << '172.22.11.2'
                    target.addresses << "roborio-${roborio.team}-frc.local".toString()
                    target.addresses << "10.${(int)(roborio.team/100)}.${((int)roborio.team)%100}.2".toString()
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
                        roborio.remote.call(target)
                    }
                }
            }
        }

        @Mutate
        void addDeployers(DeployersSpec deployers, final ExtensionContainer extensions) {
            FRCExtension frcExt = extensions.getByName('frc')
            Project project = extensions.getByName('projectWrapper').project

            ExportJarResourceTask netconsolehost_task = project.tasks.create('exportNetconsoleHost', ExportJarResourceTask) { task ->
                task.resource = "netconsole/netconsole-host"
                task.outfile = new File(project.buildDir, "gradlerio/resource/${task.resource}")
            }

            def nativeZips = project.configurations.nativeZip
            def zips = nativeZips.dependencies.findAll { dep -> dep != null && nativeZips.files(dep).size() > 0 }.collect { dep ->
                return [dep, project.zipTree(nativeZips.files(dep).first())]
            }

            // Create the "Java Libraries" deployer. This avoids deploying libraries more than once.
            if (frcExt.java.size() > 0) {
                deployers.create('javaLibraries') { deployer ->
                    deployer.targets += frcExt.java.collectMany { j -> j.targets }
                    deployer.predeploy << "echo 'Deploying Java Libraries'"
                    deployer.order = 1
                    // ARTIFACT: Netconsole
                    deployer.artifacts.create("netconsolehost", FileArtifact) { artifact ->
                        artifact.predeploy << "killall -q netconsole-host 2> /dev/null || :"
                        artifact.file = netconsolehost_task.outputs.files.first()
                        artifact.directory = '/usr/local/frc/bin'
                        artifact.filename = 'netconsole-host'
                        artifact.postdeploy << 'chmod +x netconsole-host'
                    }

                    // Add netconsolehost export task dependency for this deployer
                    project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { t ->
                        t.dependsOn netconsolehost_task
                        println(t)
                    }

                    // Add native libs (single .so)
                    def nativeLibs = project.configurations.nativeLib
                    nativeLibs.dependencies.findAll { dep -> dep != null && nativeLibs.files(dep).size() > 0 }.forEach { dep ->
                        def libfile = nativeLibs.files(dep).first()
                        // ARTIFACT: Native Libs
                        deployer.artifacts.create("nativeLib${dep.name.capitalize()}", FileArtifact) { artifact ->
                            artifact.file = libfile
                            artifact.directory = '/usr/local/frc/lib'
                            artifact.cacheMethod = CacheMethod.MD5_FILE
                        }
                    }

                    // Add native zips (from wpilib etc)
                    zips.forEach { zipentry ->
                        def zipdep = zipentry.first()
                        FileTree ziptree = zipentry.last()

                        Set<File> nativelibs = ["", "lib", "java/lib", "linux/athena"].collectMany { dirext ->
                            ziptree.matching { pat -> pat.include("${dirext}${dirext.length() > 0 ? "/" : ""}*.so") }.getFiles()
                        }

                        // ARTIFACT: Native Zips
                        deployer.artifacts.create("nativeZip${zipdep.name.capitalize()}", FileSetArtifact) { artifact ->
                            artifact.files = nativelibs
                            artifact.directory = '/usr/local/frc/lib'
                            artifact.cacheMethod = CacheMethod.MD5_FILE
                        }
                    }

                    // Add unzip tasks as dependencies to the main deploy task for this deployer
                    project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                        zips.collect { z -> z[1] }.forEach { zt -> task.dependsOn zt }
                    }
                }
            }

            def createBaseDeployer = { Deployer deployer, FRCExtConfig ext ->
                deployer.targets += ext.targets
                deployer.predeploy << "whoami"
                deployer.postdeploy << "ldconfig" << "sync" << ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r || true"
            }

            frcExt.java.each { FRCJava java ->
                deployers.create(java.name) { deployer ->
                    createBaseDeployer(deployer, java)
                    project.plugins.getPlugin(FRCJavaPlugin).configureDeployer(project, deployer, java)
                    project.tasks.matching { t -> t.name == "deploy${deployer.name.capitalize()}".toString() }.whenTaskAdded { task ->
                        task.dependsOn('deployJavaLibraries')
                    }
                }
            }

            frcExt.nativ.each { FRCNative nativ ->
                deployers.create(nativ.name) { deployer ->
                    createBaseDeployer(deployer, nativ)
                    project.plugins.getPlugin(FRCNativePlugin).configureDeployer(project, deployer, nativ)
                }
            }
        }
    }
}