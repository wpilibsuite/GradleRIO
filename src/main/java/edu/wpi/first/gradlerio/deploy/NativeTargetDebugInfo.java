package edu.wpi.first.gradlerio.deploy;

public class NativeTargetDebugInfo extends TargetDebugInfo {
    public final String type = "native";
    public int port;
    public String target;

    public NativeTargetDebugInfo(int port, String target) {
        this.port = port;
        this.target = target;
    }
}
