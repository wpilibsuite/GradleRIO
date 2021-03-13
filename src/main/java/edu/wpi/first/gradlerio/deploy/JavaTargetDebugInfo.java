package edu.wpi.first.gradlerio.deploy;

public class JavaTargetDebugInfo extends TargetDebugInfo {
    public final String type = "java";
    public int port;
    public String target;

    public JavaTargetDebugInfo(int port, String target) {
        this.port = port;
        this.target = target;
    }
}
