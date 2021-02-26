package edu.wpi.first.gradlerio.wpi.simulation;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import org.gradle.api.Project;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;

public class SimulationExtension {
    private Map<String, String> environment = new HashMap<>();

    public Map<String, String> getEnvironment() {
        return environment;
    }

    void envVar(String name, String value) {
        environment.put(name, value);
    }

    private NamedDomainObjectContainer<HalSimExtension> halExtensions;

    @Inject
    public SimulationExtension(Project project) {
        halExtensions = project.getObjects().domainObjectContainer(HalSimExtension.class, name -> {
            return project.getObjects().newInstance(HalSimExtension.class, name);
        });
    }

    public NamedDomainObjectContainer<HalSimExtension> getHalExtensions() {
        return halExtensions;
    }

    void halExtensions(final Action<? super NamedDomainObjectContainer<HalSimExtension>> closure) {
        closure.execute(halExtensions);
    }
}
