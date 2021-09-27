package edu.wpi.first.gradlerio.deploy;

import edu.wpi.first.deployutils.deploy.artifact.Artifact;

public interface DebuggableArtifact extends Artifact {
    TargetDebugInfo getTargetDebugInfo();
}
