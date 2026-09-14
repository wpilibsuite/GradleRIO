package org.wpilib.gradlerio.deploy;

import org.wpilib.deployutils.deploy.artifact.Artifact;

public interface DebuggableArtifact extends Artifact {
    TargetDebugInfo getTargetDebugInfo();
}
