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

    }

    int team
    void setTeam(int team) {
        this.team = team
        this.addresses = [ "roborio-${team}-frc.local".toString(), "10.${(int)(team / 100)}.${team % 100}.2".toString(), "172.22.11.2" ]
    }
}
