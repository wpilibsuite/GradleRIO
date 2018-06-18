package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget

@CompileStatic
class RoboRIO extends RemoteTarget {

    RoboRIO(String name) {
        super(name)
        this.directory = '/home/lvuser'
        this.user = 'admin'
        this.password = ''

        // 2 Core RoboRIO processor. You can increase this number, but depending on your machine,
        // your network connection, your code CPU usage and other factors, you may find deploys
        // start failing since there are too many SSH sessions open at once.
        this.maxChannels = 2
    }

    int team
    void setTeam(int team) {
        this.team = team
        this.addresses = [ "roborio-${team}-FRC.local".toString(), "10.${(int)(team / 100)}.${team % 100}.2".toString(), "172.22.11.2" ]
    }
}
