package edu.wpi.first.gradlerio.deploy;

import edu.wpi.first.deployutils.deploy.artifact.JavaArtifact;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public class DebuggableJavaArtifact extends JavaArtifact {

    private int debugPort = 8349;

    public int getDebugPort() {
        return debugPort;
    }

    public DebuggableJavaArtifact(String name, RemoteTarget target) {
        super(name, target);
    }

}
