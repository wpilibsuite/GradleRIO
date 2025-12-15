package org.wpilib.gradlerio.deploy.systemcore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.wpilib.deployutils.deploy.target.RemoteTarget;
import org.wpilib.deployutils.deploy.target.discovery.action.DiscoveryAction;
import org.wpilib.deployutils.deploy.target.discovery.action.SshDiscoveryAction;
import org.wpilib.deployutils.deploy.target.location.SshDeployLocation;
import org.wpilib.deployutils.log.ETLogger;
import org.wpilib.deployutils.log.ETLoggerFactory;

public class DSDeployLocation extends SshDeployLocation {

    @Inject
    public DSDeployLocation(String name, RemoteTarget target) {
        super(name, target);
    }

    private String dsAddress = "localhost";
    private Optional<String> cachedAddress = Optional.empty();
    private ETLogger log = ETLoggerFactory.INSTANCE.create("DSDeployLocation");
    private int timeout = 1000;
    private int port = 1742;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String getAddress() {
        return cachedAddress.orElseGet(this::determineAddress);
    }

    @Override
    public void setAddress(String address) {
        this.dsAddress = address;
    }

    @Override
    public DiscoveryAction createAction() {
        return new SshDiscoveryAction(this);
    }

    private static class DsJsonData {
        @SerializedName("FMS Connected") public Boolean fmsConnected;
        public Long robotIP;
    }

    private String determineAddress() {
        try (Socket socket = new Socket()) {
            InetAddress addr = InetAddress.getByName(dsAddress);
            socket.connect(new InetSocketAddress(addr, port), timeout);

            String jsonText = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
            Gson gson = new Gson();
            DsJsonData data = gson.fromJson(jsonText, DsJsonData.class);

            if (data.fmsConnected) {
                String msg = "You can't deploy code while connected to the FMS! Ask the FTA to allow you to tether your robot.";
                log.logErrorHead(msg);
                throw new FMSConnectedException(msg);
            }

            if (data.robotIP != null) {
                long ipLong = data.robotIP;
                if (ipLong == 0) {
                    log.debug("Driver Station isn't connected to robot.");
                    return "ds_comms_failed.ds_not_connected_to_robot";
                }

                InetAddress ip = InetAddress.getByAddress(new byte[] {
                    (byte) ((ipLong >> 24) & 0xff),
                    (byte) ((ipLong >> 16) & 0xff),
                    (byte) ((ipLong >> 8) & 0xff),
                    (byte) (ipLong & 0xff)
                });

                log.log("Driver Station reported IP: " + ip.getHostAddress());
                cachedAddress = Optional.of(ip.getHostAddress());
                return ip.getHostAddress();
            } else {
                log.debug("Driver Station didn't provide robotIP in JSON response");
            }
            return "ds_comms_failed.no_address";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
