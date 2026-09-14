package org.wpilib.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;
import org.wpilib.deployutils.deploy.DeployExtension;
import org.wpilib.deployutils.deploy.target.location.SshDeployLocation;
import org.wpilib.gradlerio.deploy.WPILibExtension;
import org.wpilib.gradlerio.deploy.WPIRemoteTarget;
import org.wpilib.toolchain.NativePlatforms;

public class SystemCore extends WPIRemoteTarget {

    private int team;
    private String username = "systemcore";
    private String password = "systemcore";

    private final RobotProgramKillArtifact programKillArtifact;
    private final RobotProgramStartArtifact programStartArtifact;

    @Inject
    public SystemCore(String name, Project project, DeployExtension de, WPILibExtension firstExtension) {
        super(name, project, de, firstExtension);

        setDirectory("/home/systemcore");

        setMaxChannels(4);

        // Increase timeout. The only time this is really used is if the host is resolved,
        // but takes forever to connect, which can happen if the CPU is loaded.
        setTimeout(7);

        programKillArtifact = project.getObjects().newInstance(RobotProgramKillArtifact.class, "programKill" + name, this);
        programStartArtifact = project.getObjects().newInstance(RobotProgramStartArtifact.class, "programStart" + name, this);

        getTargetPlatform().set(NativePlatforms.systemcore);

        getArtifacts().add(programKillArtifact);
        getArtifacts().add(programStartArtifact);
    }

    public RobotProgramKillArtifact getProgramKillArtifact() {
        return programKillArtifact;
    }

    public RobotProgramStartArtifact getProgramStartArtifact() {
        return programStartArtifact;
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

    public void useDefaultSystemcoreHostName() {
        this.addAddress("robot.local");
    }

    public void useCustomSystemcoreHostName(String hostName) {
        this.addAddress(hostName);
    }

    public void setTeam(int team) {
        this.team = team;
        OperatingSystem.current().isWindows();

        setAddresses(
            "10." + (team / 100) + "." + (team % 100) + ".2", // 10.TE.AM.2
            OperatingSystem.current().isWindows() ? "172.26.0.1" : "172.27.0.1", // Windows USB : Unix USB
            "172.30.0.1" // WiFi
        );
    }

    public void setAddresses(String... addresses) {
        this.getLocations().clear();

        for (String addr : addresses) {
            this.addAddress(addr);
        }

        getLocations().create("ds", FirstDsDeployLocation.class, ds -> {
            ds.setUser(username);
            ds.setPassword(password);
            ds.setIpv6(false);
        });

        getLocations().create("nids", NiDsDeployLocation.class, ds -> {
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
