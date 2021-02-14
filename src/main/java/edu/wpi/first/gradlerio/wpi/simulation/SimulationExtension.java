package edu.wpi.first.gradlerio.wpi.simulation;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.NamedDomainObjectContainer;

public class SimulationExtension {
    private Map<String, String> environment = new HashMap<>();

    public Map<String, String> getEnvironment() {
        return environment;
    }

    void envVar(String name, String value) {
        environment.put(name, value);
    }


    private NamedDomainObjectContainer<HalSimExtension> extensions;

    @Inject
    public SimulationExtension(Project project) {
        extensions = project.getObjects().domainObjectContainer(HalSimExtension.class);
    }
}