package edu.wpi.first.gradlerio.wpi.java;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.jvm.tasks.Jar;

import edu.wpi.first.gradlerio.simulation.JavaExternalSimulationTask;
import edu.wpi.first.gradlerio.wpi.WPIPlugin;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
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

    private final WPIJavaDepsExtension deps;
    private final WPIJavaVendorDepsExtension vendor;
    private final WPIJavaExecutionExtension execution;

    public WPIJavaDepsExtension getDeps() {
        return deps;
    }

    public WPIJavaVendorDepsExtension getVendor() {
        return vendor;
    }

    public WPIJavaExecutionExtension getExecution() {
        return execution;
    }

    private final Configuration debugNativeConfiguration;
    private final Configuration releaseNativeConfiguration;

    public Configuration getDebugNativeConfiguration() {
        return debugNativeConfiguration;
    }

    public Configuration getReleaseNativeConfiguration() {
        return releaseNativeConfiguration;
    }

    private final FileCollection debugFileCollection;
    private final FileCollection releaseFileCollection;

    public FileCollection getDebugFileCollection() {
        return debugFileCollection;
    }

    public FileCollection getReleaseFileCollection() {
        return releaseFileCollection;
    }

    private final Property<Boolean> debugJni;

    public Property<Boolean> getDebugJni() {
        return debugJni;
    }

    public TaskProvider<JavaExternalSimulationTask> externalSimulationTask;

    public TaskProvider<JavaExternalSimulationTask> getExternalSimulationTask() {
        return externalSimulationTask;
    }

    public void enableExternalTasks(Jar jar) {
        externalSimulationTask.configure(x -> x.getJars().add(jar));
    }

    @Inject
    public WPIJavaExtension(Project project, SimulationExtension sim, WPIVersionsExtension versions, WPIVendorDepsExtension vendorDeps) {
        extractNativeDebugArtifacts = project.getTasks().register("extractDebugNative", ExtractNativeJavaArtifacts.class);
        extractNativeReleaseArtifacts = project.getTasks().register("extractReleaseNative", ExtractNativeJavaArtifacts.class);

        debugJni = project.getObjects().property(Boolean.class);
        debugJni.set(false);
        deps = project.getObjects().newInstance(WPIJavaDepsExtension.class, versions);
        vendor = project.getObjects().newInstance(WPIJavaVendorDepsExtension.class, vendorDeps, versions);
        execution = project.getObjects().newInstance(WPIJavaExecutionExtension.class, project, this);

        debugNativeConfiguration = project.getConfigurations().create("nativeDebug");
        releaseNativeConfiguration = project.getConfigurations().create("nativeRelease");

        PatternFilterable filterable = new PatternSet();
        filterable.include("**/*.so", "**/*.dylib", "**/*.pdb", "**/*.dll");

        ArtifactView debugView = debugNativeConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        ArtifactView releaseView = releaseNativeConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        Callable<Set<File>> debugCallable = () -> debugView.getFiles().getAsFileTree().matching(filterable).getFiles();
        Callable<Set<File>> releaseCallable = () -> releaseView.getFiles().getAsFileTree().matching(filterable).getFiles();

        debugFileCollection = project.files(debugCallable);
        releaseFileCollection = project.files(releaseCallable);

        extractNativeDebugArtifacts.configure(extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/debug"));
            extract.getFiles().from(sim.getDebugFileCollection());
            extract.getFiles().from(debugFileCollection);
        });

        extractNativeReleaseArtifacts.configure(extract -> {
            extract.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("jni/release"));
            extract.getFiles().from(sim.getReleaseFileCollection());
            extract.getFiles().from(releaseFileCollection);
        });

        externalSimulationTask = project.getTasks().register("simulateExternalJava", JavaExternalSimulationTask.class, t -> {
            t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("sim/java.json"));
        });
    }
}
