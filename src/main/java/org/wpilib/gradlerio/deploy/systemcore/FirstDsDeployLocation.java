package org.wpilib.gradlerio.deploy.systemcore;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;

import javax.inject.Inject;

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

    private byte[] lengthBytes = new byte[2];
    byte[] tagBytes = new byte[100];

    private String readTag(InputStream input) throws IOException {
        int read = input.readNBytes(lengthBytes, 0, lengthBytes.length);
        if (read != lengthBytes.length) {
            log.debug("Failed to read tag length from Driver Station");
            throw new IOException("Failed to read tag length from Driver Station");
        }
        int tagLength = ((lengthBytes[0] & 0xFF) << 8) | (lengthBytes[1] & 0xFF);

        if (tagLength > tagBytes.length) {
            tagBytes = new byte[tagLength];
        }

        read = input.readNBytes(tagBytes, 0, tagBytes.length);
        if (read != tagBytes.length) {
            throw new IOException("Failed to read full tag from Driver Station");
        }

        if (tagBytes[0] != 50) {
            return null;
        }

        if ((tagBytes[5] & 1) != 0) {
            String msg = "You can't deploy code while connected to the FMS! Ask the FTA to allow you to tether your robot.";
            log.logErrorHead(msg);
            throw new FMSConnectedException(msg);
        }

        long ipLong = tagBytes[1] & 0xFFL << 24 | (tagBytes[2] & 0xFFL) << 16 | (tagBytes[3] & 0xFFL) << 8 | (tagBytes[4] & 0xFFL);
        if (ipLong == 0) {
            log.debug("FIRST Driver Station isn't connected to robot.");
            return "first_ds_comms_failed.ds_not_connected_to_robot";
        }

        InetAddress ip = InetAddress.getByAddress(new byte[] {
            (byte) ((ipLong >> 24) & 0xff),
            (byte) ((ipLong >> 16) & 0xff),
            (byte) ((ipLong >> 8) & 0xff),
            (byte) (ipLong & 0xff)
        });

        log.log("FIRST Driver Station reported IP: " + ip.getHostAddress());
        return ip.getHostAddress();
    }

    private String determineAddress() {
        log.log("FIRST Driver Station: Attempting to determine robot IP address");
        try (Socket socket = new Socket()) {
            InetAddress addr = InetAddress.getByName(dsAddress);
            socket.connect(new InetSocketAddress(addr, port), timeout);

            socket.setSoTimeout(timeout);

            while (true) {
                String result = readTag(socket.getInputStream());
                if (result != null) {
                    cachedAddress = Optional.of(result);
                    return result;
                }
            }
        } catch (SocketTimeoutException e) {
            log.debug("Timed out while trying to connect to FIRST Driver Station. Is the IP address correct and is the FIRST Driver Station running?");
            cachedAddress = Optional.of("first_ds_comms_failed.no_address");
            return "first_ds_comms_failed.no_address";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
