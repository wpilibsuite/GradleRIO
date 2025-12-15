package org.wpilib.gradlerio.deploy;

public class JavaTargetDebugInfo extends TargetDebugInfo {
    public final String type = "java";
    public String name;
    public int port;
    public String target;
    public String project;

    public JavaTargetDebugInfo(String name, int port, String target, String project) {
        this.name = name;
        this.port = port;
        this.target = target;
        this.project = project;
    }
}
