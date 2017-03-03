package jaci.openrio.gradle.frc

import org.gradle.api.*;
import groovy.util.*;

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.*
import org.hidetake.groovy.ssh.connection.*

class DeployPlugin implements Plugin<Project> {
    def robotCommand = { runargs, jvmargs, binary ->
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs} -jar ${binary} ${runargs}"
    }

    def robotDebugCommand = { runargs, binary -> 
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs} -XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=8348,server=y,suspend=y -jar ${binary} ${runargs}"
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
                if (project.frc.deploy) {
                    def file = project.jar.archivePath
                    project.deploy_ssh.run {
                        session(host: project.frc._active_robot_address, user: 'admin', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                            def conf = project.configurations.nativeLib
                            execute "mkdir -p /usr/local/frc/lib/_gradlerio"

                            conf.dependencies.findAll { it != null }.collect {
                                def libfile = conf.files(it)[0]
                                project.ant.checksum(file: libfile)
                                def check = new File("${libfile.absolutePath}.MD5").text.trim();
                                def riocheck = execute "cat /usr/local/frc/lib/_gradlerio/${libfile.name}.MD5 2> /dev/null || echo 'none'", ignoreError: true
                                
                                if (check != riocheck.trim()) {
                                    println "RoboRIO Library ${libfile.name} out of date! Updating Library"
                                    put from: libfile, into: "/usr/local/frc/lib"
                                    put from: "${libfile.path}.MD5", into: "/usr/local/frc/lib/_gradlerio"
                                }
                            }

                            def confZip = project.configurations.nativeZip
                            confZip.dependencies.findAll { it != null }.collect {
                                def zipfile = confZip.files(it)[0]
                                def ziplocal = new File(project.buildDir, "depUnzip/${zipfile.name}")
                                project.ant.unzip(src: zipfile,
                                    dest: ziplocal,
                                    overwrite: "true")
                                project.fileTree(ziplocal).include("*.so*").visit { vis ->
                                    project.ant.checksum(file: vis.file)
                                    def check = new File("${vis.file.absolutePath}.MD5").text.trim();
                                    def riocheck = execute "cat /usr/local/frc/lib/_gradlerio/${vis.file.name}.MD5 2> /dev/null || echo 'none'", ignoreError: true
                                    
                                    if (check != riocheck.trim()) {
                                        println "RoboRIO Library ${vis.file.name} out of date! Updating Library"
                                        put from: vis.file, into: "/usr/local/frc/lib"
                                        put from: "${vis.file.path}.MD5", into: "/usr/local/frc/lib/_gradlerio"
                                    }
                                }
                                project.fileTree(new File(ziplocal, "lib")).include("*.so*").visit { vis ->
                                    project.ant.checksum(file: vis.file)
                                    def check = new File("${vis.file.absolutePath}.MD5").text.trim();
                                    def riocheck = execute "cat /usr/local/frc/lib/_gradlerio/${vis.file.name}.MD5 2> /dev/null || echo 'none'", ignoreError: true
                                    
                                    if (check != riocheck.trim()) {
                                        println "RoboRIO Library ${vis.file.name} out of date! Updating Library"
                                        put from: vis.file, into: "/usr/local/frc/lib"
                                        put from: "${vis.file.path}.MD5", into: "/usr/local/frc/lib/_gradlerio"
                                    }
                                }
                                project.fileTree(new File(ziplocal, "java/lib")).include("*.so*").visit { vis ->
                                    project.ant.checksum(file: vis.file)
                                    def check = new File("${vis.file.absolutePath}.MD5").text.trim();
                                    def riocheck = execute "cat /usr/local/frc/lib/_gradlerio/${vis.file.name}.MD5 2> /dev/null || echo 'none'", ignoreError: true
                                    
                                    if (check != riocheck.trim()) {
                                        println "RoboRIO Library ${vis.file.name} out of date! Updating Library"
                                        put from: vis.file, into: "/usr/local/frc/lib"
                                        put from: "${vis.file.path}.MD5", into: "/usr/local/frc/lib/_gradlerio"
                                    }
                                }
                            }

                            execute "killall -q netconsole-host 2> /dev/null || :", ignoreError: true       // Kill netconsole
                            def instream = DeployPlugin.class.getClassLoader().getResourceAsStream("netconsole/netconsole-host")
                            put from: instream, into: "/usr/local/frc/bin/netconsole-host"
                            instream = DeployPlugin.class.getClassLoader().getResourceAsStream("netconsole/netconsole-host.properties")
                            put from: instream, into: "/usr/local/frc/bin/netconsole-host.properties"
                            execute "chmod +x /usr/local/frc/bin/netconsole-host /usr/local/frc/bin/netconsole-host.properties"
                            
                            execute "ldconfig"
                        }
                        session(host: project.frc._active_robot_address, user: 'lvuser', timeoutSec: project.frc.deployTimeout, knownHosts: AllowAnyHosts.instance) {
                            execute ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t 2> /dev/null", ignoreError: true        // Kill user code
                            execute "mkdir -p ${project.frc.deployDirectory}"
                            put from: file, into: project.frc.deployDirectory
                            def binname = "${project.frc.deployDirectory}/${file.name}"
                            execute "chmod +x ${binname}"

                            if (project.frc.robotCommand != null) {
                                def cmd = ""
                                if (project.frc.robotCommand == "") {
                                    if (project.frc.useDebugCommand)
                                        cmd = robotDebugCommand(project.frc.runArguments, project.frc.jvmArguments, binname)
                                    else
                                        cmd = robotCommand(project.frc.runArguments, project.frc.jvmArguments, binname)
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