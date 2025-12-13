package org.wpilib.gradlerio.simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;
import org.wpilib.gradlerio.wpi.WPIExtension;
import org.wpilib.gradlerio.wpi.java.ExtractNativeJavaArtifacts;
import org.wpilib.gradlerio.wpi.simulation.SimulationExtension;

public class JavaExternalSimulationTask extends DefaultTask {
    private final List<Jar> jars = new ArrayList<>();
    private Provider<ExtractNativeJavaArtifacts> extractJni;
    private boolean isDebug;

    @Internal
    public List<Jar> getJars() {
        return jars;
    }

    public void setDependencies(SimulationExtension sim, Provider<ExtractNativeJavaArtifacts> extract, boolean debug, Project project) {
        this.extractJni = extract;
        isDebug = debug;
        this.dependsOn(extractJni);
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

    public static class SimInfo {
        public final String type = "java";
        public final String name;
        public final List<HalSimPair> extensions;
        public final Map<String, String> environment;
        public final String libraryDir;
        public final String mainClassName;

        public SimInfo(String name, List<HalSimPair> extensions, Map<String, String> environment, String libraryDir,
                String mainClassName) {
            this.name = name;
            this.extensions = extensions;
            this.environment = environment;
            this.libraryDir = libraryDir;
            this.mainClassName = mainClassName;
        }
    }

    @TaskAction
    public void execute() throws IOException {
        var ext = getProject().getExtensions().getByType(WPIExtension.class);
        SimulationExtension sim = ext.getSim();

        File ldpath = extractJni.get().getDestinationDirectory().get().getAsFile();

        List<SimInfo> simInfo = new ArrayList<>();

        List<HalSimPair> extensions = sim.getHalSimLocations(List.of(ldpath), isDebug);

        Map<String, String> env = sim.getEnvironment();

        for (Jar jar : jars) {
            String name =  jar.getName() + " (in project " + getProject().getName() + ")";

            String mainClass = (String)jar.getManifest().getAttributes().get("Main-Class");

            simInfo.add(new SimInfo(name, extensions, env, ldpath.getAbsolutePath(), mainClass));
        }

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = simulationFile.get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(simInfo));
    }
}
