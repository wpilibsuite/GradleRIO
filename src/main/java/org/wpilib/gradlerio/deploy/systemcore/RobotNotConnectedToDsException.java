package org.wpilib.gradlerio.deploy.systemcore;

public class RobotNotConnectedToDsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RobotNotConnectedToDsException(String msg) {
        super(msg);
    }
}
