package edu.wpi.first.gradlerio.deploy.roborio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.location.SshDeployLocation;
import edu.wpi.first.gradlerio.deploy.FRCExtension;
import edu.wpi.first.gradlerio.deploy.WPIRemoteTarget;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.toolchain.NativePlatforms;

public class RoboRIO extends WPIRemoteTarget {

    private final Logger log;
    private int team;
    private boolean checkImage = true;
    private final List<String> validImageVersions;

    private final FRCProgramKillArtifact programKillArtifact;

    @Inject
    public RoboRIO(String name, Project project, DeployExtension de, FRCExtension frcExtension) {
        super(name, project, de, frcExtension);
        log = Logging.getLogger(this.toString());

        setDirectory("/home/lvuser");

        setMaxChannels(4);

        // Increase timeout. The only time this is really used is if the host is resolved,
        // but takes forever to connect, which can happen if the CPU is loaded.
        setTimeout(7);

        setOnlyIf(ctx -> verifyOnlyIf(ctx));

        // Make a copy of valid image versions so user defined cannot modify the global array
        validImageVersions = new ArrayList<>(WPIExtension.getValidImageVersions());

        programKillArtifact = project.getObjects().newInstance(FRCProgramKillArtifact.class, "programKill" + name, this);

        getTargetPlatform().set(NativePlatforms.roborio);

        getArtifacts().add(programKillArtifact);
    }

    public FRCProgramKillArtifact getProgramKillArtifact() {
        return programKillArtifact;
    }

    public List<String> getValidImageVersions() {
        return validImageVersions;
    }

    public boolean isCheckImage() {
        return checkImage;
    }

    public void setCheckImage(boolean checkImage) {
        this.checkImage = checkImage;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
        setAddresses(
            "roborio-" + team + "-FRC.local", // Default mDNS
            "10." + (team / 100) + "." + (team % 100) + ".2", // 10.TE.AM.2
            "172.22.11.2", // USB

            // Weird Environments
            "roborio-" + team + "-FRC", // Default DNS
            "roborio-" + team + "-FRC.lan", // LAN mDNS/DNS
            "roborio-" + team + "-FRC.frc-field.local" /// Practice Field mDNS
        );
    }

    public void setAddresses(String... addresses) {
        this.getLocations().clear();

        for (String addr : addresses) {
            this.addAddress(addr);
        }

        getLocations().create("ds", DSDeployLocation.class, ds -> {
            ds.setUser("admin");
            ds.setPassword("");
            ds.setIpv6(false);
        });
    }

    public void addAddress(String address) {
        getLocations().create(address, SshDeployLocation.class, loc -> {
            loc.setAddress(address);
            loc.setIpv6(false);
            loc.setUser("admin");
            loc.setPassword("");
        });
    }

    private boolean verifyOnlyIf(DeployContext ctx) {
        ctx.getLogger().silent(true);
        try {
            if (checkImage) {
                log.info("Checking image...");
                readAndVerifyImage(ctx);
            }
        } finally {
            ctx.getLogger().silent(false);
        }
        return true;
    }

    private void readAndVerifyImage(DeployContext context) {
        final String imageFile = "/etc/natinst/share/scs_imagemetadata.ini";
        final Pattern pattern = Pattern.compile("^IMAGEVERSION\\s*=\\s*\\\"(FRC_)?roboRIO2?_(?<version>\\d{4}_v\\d+(?:\\.\\d+)?)\\\"");

        String content = context.execute("cat " + imageFile).getResult();
        log.info("Received Image File: ");
        log.info(content);

        boolean imageFound = false;
        for (String line : content.split("\n")) {
            log.info(line);
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.matches()) {
                String imageGroup = matcher.group("version");
                log.info("Matched version: " + imageGroup);
                verifyImageVersion(imageGroup);
                imageFound = true;
                break;
            }
        }

        if (!imageFound) {
            throw new InvalidImageException(content);
        }
    }

    private void verifyImageVersion(String image) {
        boolean foundMatch = validImageVersions.stream().filter(x -> {
            int index = x.indexOf("*");
            if (index == -1) {
                // no wildcard, check if versions are equal
                return x.equals(image);
            } else if (index > image.length()) {
                return false;
            } else {
                return (x.substring(0, index).equals(image.substring(0, index)));
            }
        }).findAny().isPresent();
        if (!foundMatch) {
            throw new InvalidImageException(image, validImageVersions);
        }
    }

    @Override
    public String toString() {
        return "RoboRIO[" + getName() + "]";
    }
}
