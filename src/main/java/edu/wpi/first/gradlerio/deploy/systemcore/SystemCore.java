package edu.wpi.first.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.target.location.SshDeployLocation;
import edu.wpi.first.gradlerio.deploy.FRCExtension;
import edu.wpi.first.gradlerio.deploy.WPIRemoteTarget;
import edu.wpi.first.toolchain.NativePlatforms;

public class SystemCore extends WPIRemoteTarget {

    private int team;
    private String username = "systemcore";
    private String password = "systemcore";

    @Inject
    public SystemCore(String name, Project project, DeployExtension de, FRCExtension frcExtension) {
        super(name, project, de, frcExtension);

        setDirectory("/home/systemcore");

        setMaxChannels(4);

        // Increase timeout. The only time this is really used is if the host is resolved,
        // but takes forever to connect, which can happen if the CPU is loaded.
        setTimeout(7);

        getTargetPlatform().set(NativePlatforms.systemcore);
    }

    void setUsername(String username) {
        this.username = username;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
        setAddresses(
            "10." + (team / 100) + "." + (team % 100) + ".2", // 10.TE.AM.2
            "robot.local",
            "172.28.0.1", // USB
            "172.30.0.1" // WiFi

            // // Weird Environments
            // "roborio-" + team + "-FRC", // Default DNS
            // "roborio-" + team + "-FRC.lan", // LAN mDNS/DNS
            // "roborio-" + team + "-FRC.frc-field.local" /// Practice Field mDNS
        );
    }

    public void setAddresses(String... addresses) {
        this.getLocations().clear();

        for (String addr : addresses) {
            this.addAddress(addr);
        }

        getLocations().create("ds", DSDeployLocation.class, ds -> {
            ds.setUser(username);
            ds.setPassword(password);
            ds.setIpv6(false);
        });
    }

    public void addAddress(String address) {
        getLocations().create(address, SshDeployLocation.class, loc -> {
            loc.setAddress(address);
            loc.setIpv6(false);
            loc.setUser(username);
            loc.setPassword(password);
        });
    }

    @Override
    public String toString() {
        return "SystemCore[" + getName() + "]";
    }
}
