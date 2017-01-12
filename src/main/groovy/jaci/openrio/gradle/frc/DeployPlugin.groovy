package jaci.openrio.gradle.frc

import org.gradle.api.*;
import groovy.util.*;

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.*
import org.hidetake.groovy.ssh.connection.*

class DeployPlugin implements Plugin<Project> {
    def robotCommand = { runargs, binary ->
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ -jar ${binary} ${runargs}"
    }

    def robotDebugCommand = { runargs, binary -> 
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ -XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=8348,server=y,suspend=y -jar ${binary} ${runargs}"
    }

    void apply(Project project) {
        project.extensions.create("frc", FRCExtension)
        project.extensions.deploy_ssh = Ssh.newService()

        def det_rio_address = project.task('determine_rio_address') {
            group "GradleRIO"
            description "Determine the active address for the RoboRIO"
            doLast {
                println "============ FINDING ROBORIO ============"
                def address = [
                    "mDNS" : getRioHost(project),
                    "USB" : "172.22.11.2",
                    "Static IP" : getRioIP(project) 
                ]
                address.any { name, addr ->
                    println "-> ${name} (${addr})..."
                    if (runSshTest(project, addr)) {
                        project.frc._active_robot_address = addr
                        println "============ ROBORIO FOUND ============"
                        return true
                    }
                }
            }
        }

        project.task('restart_rio_code') {
            group "GradleRIO"
            description "Restart User Code running on the RoboRIO"
            dependsOn det_rio_address
            doLast {
                project.deploy_ssh.run {
                    session(host: project.frc._active_robot_address, user: 'lvuser', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                        execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r", ignoreError: true // Just in case
                    }
                }
            }
        }

        project.task('deploy') {
            group "GradleRIO"
            description "Deploy Java Code to the RoboRIO"
            dependsOn det_rio_address
            doLast {
                def file = project.jar.archivePath
                project.deploy_ssh.run {
                    session(host: project.frc._active_robot_address, user: 'admin', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                        def conf = project.configurations.native
                        conf.dependencies.findAll { it != null }.collect {
                            def libfile = conf.files(it)[0]
                            put from: libfile, into: "/usr/local/frc/lib"
                        }

                        def confZip = project.configurations.nativeZip
                        confZip.dependencies.findAll { it != null }.collect {
                            def zipfile = confZip.files(it)[0]
                            def ziplocal = new File(project.buildDir, "depUnzip/${zipfile.name}")
                            project.ant.unzip(src: zipfile,
                                dest: ziplocal,
                                overwrite: "true")
                            project.fileTree(ziplocal).include("*.so*").visit { vis ->
                                put from: vis.file, into: "/usr/local/frc/lib"
                            }
                            project.fileTree(new File(ziplocal, "lib")).include("*.so*").visit { vis ->
                                put from: vis.file, into: "/usr/local/frc/lib"
                            }
                            project.fileTree(new File(ziplocal, "java/lib")).include("*.so*").visit { vis ->
                                put from: vis.file, into: "/usr/local/frc/lib"
                            }
                        }
                        def instream = DeployPlugin.class.getClassLoader().getResourceAsStream("netconsole/netconsole-host")
                        put from: instream, into: "/usr/local/frc/bin/netconsole-host"
                        instream = DeployPlugin.class.getClassLoader().getResourceAsStream("netconsole/netconsole-host.properties")
                        put from: instream, into: "/usr/local/frc/bin/netconsole-host.properties"
                        execute "chmod +x /usr/local/frc/bin/netconsole-host /usr/local/frc/bin/netconsole-host.properties"
                        
                        execute "ldconfig"
                    }
                    session(host: project.frc._active_robot_address, user: 'lvuser', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                        execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t", ignoreError: true // Just in case
                        execute "mkdir -p ${project.frc.deployDirectory}"
                        put from: file, into: project.frc.deployDirectory
                        def binname = "${project.frc.deployDirectory}/${file.name}"
                        execute "chmod +x ${binname}"

                        if (project.frc.robotCommand != null) {
                            def cmd = ""
                            if (project.frc.robotCommand == "") {
                                if (project.frc.useDebugCommand)
                                    cmd = robotDebugCommand(project.frc.runArguments, binname)
                                else
                                    cmd = robotCommand(project.frc.runArguments, binname)
                            } else {
                                cmd = project.frc.robotCommand
                            }
                            def rc_local = new File(project.buildDir, "robotCommand")
                            rc_local.write("${cmd}\n")
                            put from: rc_local, into: "/home/lvuser"
                            execute "chmod +x /home/lvuser/robotCommand"
                        }

                        execute "sync"
                        execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r", ignoreError: true     // Restart Code
                    }
                }
            }
        }
    }

    String getRioHost(Project project) {
        return project.frc.rioHost == "" ? "roboRIO-${project.frc.team}-frc.local" : project.frc.rioHost
    }

    String getRioIP(Project project) {
        if (project.frc.rioIP == "") {
            def team = project.frc.team
            def teamlen = team.length()
            if (teamlen < 4)
                for (int i = 0; i < 4-teamlen; i++)
                    team = "0" + team
            return "10.${team.substring(0,2)}.${team.substring(2,4)}.2"
        }
        return project.frc.rioIP
    }

    boolean runSshTest(Project project, String addr) {
        try {
            def result = project.deploy_ssh.run {
                session(host: addr, user: 'lvuser', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                    println "--> SUCCESS"
                }
            }
            return true
        } catch (all) {
            return false
        }
    }
}