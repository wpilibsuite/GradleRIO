package edu.wpi.first.gradlerio.wpi.simulation;

import java.io.File;
import java.util.Optional;

import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public class HalSimExtension implements Named {
    private final String name;
    private final Property<String> groupId;
    private final Property<String> artifactId;
    private final Property<String> version;
    private final Property<Boolean> defaultEnabled;

    @Override
    public String getName() {
        return name;
    }

    public Property<String> getGroupId() {
        return groupId;
    }

    public Property<String> getArtifactId() {
        return artifactId;
    }

    public Property<String> getVersion() {
        return version;
    }

    public Property<Boolean> getDefaultEnabled() {
        return defaultEnabled;
    }

    public Provider<String> getReleaseDependency(Project project, SimulationExtension sim) {
        return project.getProviders().provider(() -> getIdString() + ":" + sim.desktopPlatform + "@zip");
    }

    public Provider<String> getDebugDependency(Project project, SimulationExtension sim) {
        return project.getProviders().provider(() -> getIdString() + ":" + sim.desktopPlatform + "debug@zip");
    }

    private String getIdString() {
        return getGroupId().get() + ":" + getArtifactId().get() + ":" + getVersion().get();
    }

    public Optional<String> getFilenameForArtifact(ArtifactView view, FileCollection files) {
        // Find artifact matching
        String idString = getIdString();
        for (ResolvedArtifactResult artifact : view.getArtifacts()) {
            if (artifact.getId().getComponentIdentifier().toString().equals(idString)) {
                // Found artifact, now find binary
                for (File file : files) {
                    if (file.getAbsolutePath().startsWith(artifact.getFile().getAbsolutePath())) {
                        return Optional.of(file.getName());
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Inject
    public HalSimExtension(String name, ObjectFactory objects) {
        this.name = name;
        groupId = objects.property(String.class);
        artifactId = objects.property(String.class);
        version = objects.property(String.class);
        defaultEnabled =  objects.property(Boolean.class);
        defaultEnabled.set(false);
    }
}
