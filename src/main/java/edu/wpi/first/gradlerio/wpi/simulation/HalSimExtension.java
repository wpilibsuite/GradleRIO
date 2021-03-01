package edu.wpi.first.gradlerio.wpi.simulation;

import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class HalSimExtension implements Named {
    private final String name;
    private final Property<String> groupId;
    private final Property<String> artifactId;
    private final Property<String> version;

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

    @Inject
    public HalSimExtension(String name, ObjectFactory objects) {
        this.name = name;
        groupId = objects.property(String.class);
        artifactId = objects.property(String.class);
        version = objects.property(String.class);
    }
}
