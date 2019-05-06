package edu.wpi.first.gradlerio.frc

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.discovery.action.SshDiscoveryAction
import jaci.gradle.deploy.target.location.SshDeployLocation
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.location.DeployLocation

import javax.inject.Inject

@CompileStatic
class DSDeployLocation extends SshDeployLocation {

    @Inject
    DSDeployLocation(RemoteTarget target) {
        super(target)
    }

    private String dsAddress = "localhost"
    private Optional<String> cachedAddress = Optional.empty()
    private ETLogger log = ETLoggerFactory.INSTANCE.create("DSDeployLocation")
    int timeout = 1000
    int port = 1742

    @Override
    public void setAddress(String addr) {
        this.dsAddress = addr
    }

    @Override
    public String getAddress() {
        cachedAddress.isPresent() ? cachedAddress.get() : determineAddress()
    }

    @Override
    public DiscoveryAction createAction() {
        return new SshDiscoveryAction(this)
    }

    private String determineAddress() {
        def socket = new Socket()
        def addr = InetAddress.getByName(dsAddress)
        socket.connect(new InetSocketAddress(addr, port), timeout)

        try {
            def ins = new BufferedInputStream(socket.getInputStream())
            def reader = new BufferedReader(new InputStreamReader(ins))

            def jsonText = reader.readLine()
            log.debug("Received string from Driver Station: ${jsonText}")
            def json = new JsonSlurper().parseText(jsonText)

            if (json["FMS Connected"]) {
                def msg = "You can't deploy code while connected to the FMS! Ask the FTA to allow you to tether your robot."
                log.logErrorHead(msg)
                throw new FMSConnectedException(msg)
            }

            if (json["robotIP"] != null) {
                long ipLong = Long.parseLong(json["robotIP"] as String)
                if (ipLong == 0) {
                    log.debug("Driver Station isn't connected to the robot.")
                    return "ds_comms_failed.ds_not_connected_to_robot"
                }
                InetAddress ip = InetAddress.getByAddress([
                        (byte) ((ipLong >> 24) & 0xff),
                        (byte) ((ipLong >> 16) & 0xff),
                        (byte) ((ipLong >> 8) & 0xff),
                        (byte) (ipLong & 0xff)] as byte[]);

                log.println("Driver Station reported IP: ${ip.hostAddress}")
                cachedAddress = Optional.of(ip.hostAddress)
                return ip.hostAddress
            } else {
                log.debug("Driver Station didn't provide robotIP in JSON response.")
            }

            return "ds_comms_failed.no_address"
        } finally {
            if (!socket.closed)
                socket.close()
        }
    }

}
