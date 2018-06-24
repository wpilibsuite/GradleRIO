package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.tasks.TargetDiscoveryTask

import java.util.regex.Matcher
import java.util.regex.Pattern

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

        this.failureMessage = { List<TargetDiscoveryTask.TargetFailedException> reasons ->
            roborioFailureMessage(reasons)
        }

        this.onlyIf = { DeployContext ctx ->
            verifyOnlyIf(ctx)
        }
    }

    int team
    void setTeam(int team) {
        this.team = team
        this.addresses = [ "roborio-${team}-FRC.local".toString(), "10.${(int)(team / 100)}.${team % 100}.2".toString(), "172.22.11.2" ]
    }

    boolean verifyOnlyIf(DeployContext ctx) {
        try {
            ctx.logger().silent(true)
            if (checkImage) {
                log.info("Checking image...")
                readAndVerifyImage(ctx);
            }
        } finally {
            ctx.logger().silent(false)
        }
        return true
    }

    void readAndVerifyImage(DeployContext context) {
        final String imageFile = "/etc/natinst/share/scs_imagemetadata.ini"
        final Pattern pattern = Pattern.compile("^IMAGEVERSION\\s=\\s\\\"FRC_roboRIO_([0-9]{4}_v[0-9]{2,})\\\"")

        String content = context.execute("cat ${imageFile}")
        log.info("Received Image File: ")
        log.info(content)

        boolean imageFound = false
        content.split("\n").each { String line ->
            Matcher matcher = pattern.matcher(line.trim())
            if (matcher.matches()) {
                String imageGroup = matcher.group(1)
                log.info("Matched version: " + imageGroup)
                verifyImageVersion(imageGroup)
                imageFound = true
            }
        }

        if (!imageFound) {
            throw new InvalidImageException()
        }
    }

    boolean checkImage = true
    List<String> validImageVersions = ["2018_v16", "2018_v17", "2018_v18"]

    void verifyImageVersion(String image) {
        if (!validImageVersions.contains(image))
            throw new InvalidImageException(image, validImageVersions)
    }

    String roborioFailureMessage(List<TargetDiscoveryTask.TargetFailedException> reasons) {
        def imageReasons = reasons.findAll { it.getCause() instanceof InvalidImageException }
        if (!imageReasons.empty) {
            return (imageReasons.first().getCause() as InvalidImageException).getMessage()
        } else {
            return "Target ${name} could not be located! ${failOnMissing ? "Failing..." : "Skipping..."}"
        }
    }

    @Override
    String toString() {
        return "RoboRIO[${name}]".toString()
    }
}
