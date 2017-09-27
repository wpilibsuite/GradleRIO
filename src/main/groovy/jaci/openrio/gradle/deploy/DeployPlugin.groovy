package jaci.openrio.gradle.deploy

import org.gradle.api.*
import groovy.util.*

import jaci.gradle.targets.*

class DeployPlugin implements Plugin<Project> {
    def robotCommand = { runargs, jvmargs, binary ->
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs} -jar ${binary} ${runargs}"
    }

    def robotDebugCommand = { runargs, binary -> 
        "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs} -XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=8348,server=y,suspend=y -jar ${binary} ${runargs}"
    }

    void apply(Project project) {
        project.pluginManager.apply('jaci.gradle.EmbeddedTools')
        project.ext.roborio = { team ->
            project.with {
                model {
                    targets {
                        "roborio_${team}"(RemoteTarget) {
                            addresses << "172.22.11.2"
                            addresses << "roborio-${team}-frc.local".toString()
                            addresses << "10.${((int)team)/100}.${((int)team)%100}.2".toString()
                            user "admin"
                            password ""
                            directory "/home/lvuser"
                            timeout 3
                            failOnMissing true
                        }
                    }
                }
            }
        }

        project.ext.roborioDeployer = { deployer ->

        }
    }
}