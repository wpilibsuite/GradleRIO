package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class WPIJavaExtension {
    private final TaskProvider<ExtractNativeJavaArtifacts> extractNativeDebugArtifacts;
    private final TaskProvider<ExtractNativeJavaArtifacts> extractNativeReleaseArtifacts;

    public TaskProvider<ExtractNativeJavaArtifacts> getExtractNativeDebugArtifacts() {
        return extractNativeDebugArtifacts;
    }

    public TaskProvider<ExtractNativeJavaArtifacts> getExtractNativeReleaseArtifacts() {
        return extractNativeReleaseArtifacts;
    }

    // TODO add WPIJavaDepsExtension and WPIJavaVendorDepsExtension

    @Inject
    public WPIJavaExtension(Project project, SimulationExtension sim) {
        extractNativeDebugArtifacts = project.getTasks().register("extractDebugNative", ExtractNativeJavaArtifacts.class, extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/debug"));
            extract.getFiles().from(sim.getDebugFileCollection());
        });

        extractNativeReleaseArtifacts = project.getTasks().register("extractReleaseNative", ExtractNativeJavaArtifacts.class, extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/release"));
            extract.getFiles().from(sim.getReleaseFileCollection());
        });
    }
}
