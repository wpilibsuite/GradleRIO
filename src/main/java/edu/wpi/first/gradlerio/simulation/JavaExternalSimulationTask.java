package edu.wpi.first.gradlerio.simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class JavaExternalSimulationTask extends DefaultTask {
    private final List<Jar> jars = new ArrayList<>();

    @Internal
    public List<Jar> getJars() {
        return jars;
    }

    @Inject
    public JavaExternalSimulationTask(ObjectFactory objects) {
        getOutputs().upToDateWhen(spec -> false);
        dependsOn(jars);
        simulationFile = objects.fileProperty();
    }

    private final RegularFileProperty simulationFile;

    @OutputFile
    public RegularFileProperty getSimulationFile() {
        return simulationFile;
    }

    public static class HalSimPair {
        public String name;
        public String libName;
    }

    public static class SimInfo {
        public final String type = "java";
        public String name;
        public List<HalSimPair> extensions;
        public Map<String, String> environment;

        public SimInfo(String name, List<HalSimPair> extensions, Map<String, String> environment) {
            this.name = name;
            this.extensions = extensions;
            this.environment = environment;
        }
    }

    @TaskAction
    public void execute() throws IOException {
        SimulationExtension sim = getProject().getExtensions().getByType(WPIExtension.class).getSim();

        List<SimInfo> simInfo = new ArrayList<>();

        List<HalSimPair> extensions = new ArrayList<>();
        Map<String, String> env = sim.getEnvironment();

        for (Jar jar : jars) {
            String name =  jar.getName() + " (in project " + getProject().getName() + ")";

            simInfo.add(new SimInfo(name, extensions, env));
        }

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = simulationFile.get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(simInfo));
    }
}
