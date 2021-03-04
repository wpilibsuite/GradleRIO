package edu.wpi.first.gradlerio.wpi.java;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.gradlerio.wpi.WPIPlugin;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;
import edu.wpi.first.toolchain.NativePlatforms;

public class WPIJavaExtension {
    private final TaskProvider<ExtractNativeJavaArtifacts> extractNativeDebugArtifacts;
    private final TaskProvider<ExtractNativeJavaArtifacts> extractNativeReleaseArtifacts;

    public TaskProvider<ExtractNativeJavaArtifacts> getExtractNativeDebugArtifacts() {
        return extractNativeDebugArtifacts;
    }

    public TaskProvider<ExtractNativeJavaArtifacts> getExtractNativeReleaseArtifacts() {
        return extractNativeReleaseArtifacts;
    }

    private final WPIJavaDepsExtension deps;
    private final WPIJavaVendorDepsExtension vendor;

    public WPIJavaDepsExtension getDeps() {
        return deps;
    }

    public WPIJavaVendorDepsExtension getVendor() {
        return vendor;
    }

    private final Configuration debugNativeConfiguration;
    private final Configuration releaseNativeConfiguration;

    public Configuration getDebugNativeConfiguration() {
        return debugNativeConfiguration;
    }

    public Configuration getReleaseNativeConfiguration() {
        return releaseNativeConfiguration;
    }

    @Inject
    public WPIJavaExtension(Project project, SimulationExtension sim, WPIVersionsExtension versions, WPIVendorDepsExtension vendorDeps) {
        deps = project.getObjects().newInstance(WPIJavaDepsExtension.class, versions);
        vendor = project.getObjects().newInstance(WPIJavaVendorDepsExtension.class, vendorDeps, versions);

        debugNativeConfiguration = project.getConfigurations().create("nativeDebug");
        releaseNativeConfiguration = project.getConfigurations().create("nativeRelease");

        extractNativeDebugArtifacts = project.getTasks().register("extractDebugNative", ExtractNativeJavaArtifacts.class, extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/debug"));
        });

        extractNativeReleaseArtifacts = project.getTasks().register("extractReleaseNative", ExtractNativeJavaArtifacts.class, extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/release"));
        });
    }
}
