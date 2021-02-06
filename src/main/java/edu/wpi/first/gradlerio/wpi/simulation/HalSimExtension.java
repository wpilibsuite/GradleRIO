package edu.wpi.first.gradlerio.wpi.simulation;

import javax.inject.Inject;
import org.gradle.api.Named;

public class HalSimExtension implements Named {
    private final String name;
    private String baseArtifact;

    @Override
    public String getName() {
        return name;
    }

    public String getBaseArtifact() {
        return baseArtifact;
    }

    public void setBaseArtifact(String baseArtifact) {
        this.baseArtifact = baseArtifact;
    }

    @Inject
    public HalSimExtension(String name) {
        this.name = name;
    }
}
