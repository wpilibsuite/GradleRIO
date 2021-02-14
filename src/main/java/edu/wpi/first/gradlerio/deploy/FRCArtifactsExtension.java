package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Action;

import edu.wpi.first.embeddedtools.deploy.artifact.ArtifactsExtension;

public class FRCArtifactsExtension {
    private final ArtifactsExtension artifacts;

    @Inject
    public FRCArtifactsExtension(ArtifactsExtension artifacts) {
        this.artifacts = artifacts;
    }

    public FRCJavaArtifact frcJavaArtifact(String name, final Action<FRCJavaArtifact> config) {
        return artifacts.artifact(name, FRCJavaArtifact.class, config);
    }
}
