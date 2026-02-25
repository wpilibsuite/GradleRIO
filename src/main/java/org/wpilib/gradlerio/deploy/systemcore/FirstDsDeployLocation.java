package org.wpilib.gradlerio.deploy.systemcore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

public class FirstDsDeployLocation extends SshDeployLocation {

    @Inject
    public FirstDsDeployLocation(String name, RemoteTarget target) {
        super(name, target);
    }

    private String dsAddress = "localhost";
    private Optional<String> cachedAddress = Optional.empty();
    private ETLogger log = ETLoggerFactory.INSTANCE.create("FirstDsDeployLocation");
    private int timeout = 1000;
    private int port = 1744;

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
        return cachedAddress.get();
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
        public Boolean fmsControlled;
        public String robotIp;
    }

    @Override
    public void discover() {
        cachedAddress = Optional.of("FIRST DS Connection Issue");
        cachedAddress = Optional.of(determineAddress());
    }

    private String determineAddress() {
        log.debug("FIRST Driver Station: Attempting to determine robot IP address");
        try (Socket socket = new Socket()) {
            InetAddress addr = InetAddress.getByName(dsAddress);
            socket.connect(new InetSocketAddress(addr, port), timeout);

            socket.setSoTimeout(timeout);

            String jsonText = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
            log.debug("FIRST Driver Station: Received JSON response: " + jsonText);
            Gson gson = new Gson();
            DsJsonData data = gson.fromJson(jsonText, DsJsonData.class);

            if (data.fmsControlled) {
                String msg = "You can't deploy code while connected to the FMS! Ask the FTA to allow you to tether your robot.";
                log.logErrorHead(msg);
                throw new FMSConnectedException(msg);
            }

            if (data.robotIp != null) {
                String ipString = data.robotIp;
                if (ipString.equals("0.0.0.0")) {
                    log.debug("FIRST Driver Station isn't connected to robot.");
                    throw new RobotNotConnectedToDsException("FIRST Driver Station isn't connected to robot.");
                }

                log.log("FIRST Driver Station reported IP: " + ipString);
                return ipString;
            } else {
                log.debug("FIRST Driver Station didn't provide robotIP in JSON response");
                throw new RuntimeException("FIRST Driver Station didn't provide robotIP in JSON response");
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                log.debug("Timed out trying to communicate with FIRST Driver Station");
                throw new RobotNotConnectedToDsException("Timed out trying to communicate with FIRST Driver Station");
            } else if (e instanceof ConnectException ex && ex.getMessage().contains("Connection refused")) {
                log.debug("Could not connect to FIRST Driver Station (connection refused)");
                throw new RobotNotConnectedToDsException("Could not connect to FIRST Driver Station (connection refused)");
            }
            throw new RuntimeException(e);
        }
    }
}
