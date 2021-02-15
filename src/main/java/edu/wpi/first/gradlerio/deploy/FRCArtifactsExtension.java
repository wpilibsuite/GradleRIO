package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Action;

import edu.wpi.first.deployutils.deploy.artifact.ArtifactsExtension;

public class FRCArtifactsExtension {
    private final ArtifactsExtension artifacts;

    @Inject
    public FRCArtifactsExtension(ArtifactsExtension artifacts) {
        this.artifacts = artifacts;
    }

    public FRCJavaArtifact frcJavaArtifact(String name, final Action<FRCJavaArtifact> config) {
        return artifacts.artifact(name, FRCJavaArtifact.class, config);
    }

    public FRCNativeArtifact frcNativeArtifact(String name, final Action<FRCNativeArtifact> config) {
        return artifacts.artifact(name, FRCNativeArtifact.class, config);
    }
}
