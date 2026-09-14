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
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.internal.JavaPluginHelper;
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.wpilib.gradlerio.wpi.WPIExtension;
import org.wpilib.gradlerio.wpi.java.ExtractNativeJavaArtifacts;
import org.wpilib.gradlerio.wpi.simulation.SimulationExtension;

public class JavaExternalSimulationTask extends DefaultTask {
    private Provider<ExtractNativeJavaArtifacts> extractJni;
    private final String projectName;
    private final WPIExtension ext;
    private JavaApplication application;
    private String taskPath;
    private Property<Integer> debugPort;
    private Property<Boolean> runSimWithDebugJni;

    @Inject
    public JavaExternalSimulationTask(ObjectFactory objects) {
        getOutputs().upToDateWhen(spec -> false);
        simulationFile = objects.fileProperty();
        this.projectName = getProject().getName();
        this.ext = getProject().getExtensions().getByType(WPIExtension.class);
        this.debugPort = objects.property(Integer.class);
        this.runSimWithDebugJni = objects.property(Boolean.class);
    }

    public void setDependencies(Provider<ExtractNativeJavaArtifacts> extract, Provider<Boolean> runSimWithDebugJni) {
        this.extractJni = extract;
        this.dependsOn(extractJni);
        this.runSimWithDebugJni.set(runSimWithDebugJni);
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
        public final String taskPath;
        public final int debugPort;

        public SimInfo(String name, List<HalSimPair> extensions, Map<String, String> environment, String libraryDir,
                String mainClassName, String taskPath, int debugPort) {
            this.name = name;
            this.extensions = extensions;
            this.environment = environment;
            this.libraryDir = libraryDir;
            this.mainClassName = mainClassName;
            this.taskPath = taskPath;
            this.debugPort = debugPort;
        }
    }

    public void setApplication(JavaApplication application) {
        this.application = application;
        JvmFeatureInternal mainFeature = JavaPluginHelper.getJavaComponent(getProject()).getMainFeature();
        dependsOn(mainFeature.getRuntimeClasspathConfiguration());
        dependsOn(mainFeature.getJarTask());
        JavaExec runTask = (JavaExec) getProject().getTasks().named("run").get();
        this.taskPath = runTask.getPath();
        this.debugPort.set(runTask.getDebugOptions().getPort());
    }

    @TaskAction
    public void execute() throws IOException {
        if (application == null) {
            throw new GradleException("Java application is not set for simulation task.");
        }

        SimulationExtension sim = ext.getSim();

        File ldpath = extractJni.get().getDestinationDirectory().get().getAsFile();

        List<SimInfo> simInfo = new ArrayList<>();

        List<HalSimPair> extensions = sim.getHalSimLocations(List.of(ldpath), runSimWithDebugJni.get());

        Map<String, String> env = sim.getEnvironment();

        String name = application.getApplicationName() + " (in project " + projectName + ")";

        String mainClass = application.getMainClass().get();

        simInfo.add(new SimInfo(name, extensions, env, ldpath.getAbsolutePath(), mainClass, taskPath, debugPort.get()));

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = simulationFile.get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(simInfo), "UTF-8");
    }
}
