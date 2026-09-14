package org.wpilib.gradlerio.deploy.systemcore;

public class FMSConnectedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FMSConnectedException(String msg) {
        super(msg);
    }
}
