package org.wpilib.gradlerio.wpi.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLogging;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.process.JavaForkOptions;
import org.gradle.internal.os.OperatingSystem;
import org.wpilib.gradlerio.simulation.HalSimPair;
import org.wpilib.gradlerio.simulation.JavaExternalSimulationTask;
import org.wpilib.gradlerio.wpi.WPIPlugin;
import org.wpilib.gradlerio.wpi.WPIVersionsExtension;
import org.wpilib.gradlerio.wpi.simulation.SimulationExtension;
import org.wpilib.nativeutils.vendordeps.WPIJavaVendorDepsExtension;
import org.wpilib.nativeutils.vendordeps.WPIVendorDepsExtension;

import com.google.gson.Gson;

public class WPIJavaExtension {
    private final Project project;
    private final SimulationExtension sim;

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

    private final FileCollection debugFileCollection;
    private final FileCollection releaseFileCollection;

    public FileCollection getDebugFileCollection() {
        return debugFileCollection;
    }

    public FileCollection getReleaseFileCollection() {
        return releaseFileCollection;
    }

    private final Property<Boolean> runSimWithDebugJni;

    public Property<Boolean> getRunSimWithDebugJni() {
        return runSimWithDebugJni;
    }

    private final TaskProvider<JavaExternalSimulationTask> externalSimulationTask;

    public TaskProvider<JavaExternalSimulationTask> getExternalSimulationTask() {
        return externalSimulationTask;
    }

    private final Provider<ExtractNativeJavaArtifacts> typedExtractNativeArtifacts;

    private static class ExternalRunConfig {
        public List<String> runningExtensions;
    }

    private void configureRunTask(JavaExec t) {
        configureExecutableNatives(t);
        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/jdk.internal.vm=ALL-UNNAMED");
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/java.lang=ALL-UNNAMED");
        jvmArgs.add("--enable-native-access=ALL-UNNAMED");
        if (OperatingSystem.current().isMacOsX()) {
            jvmArgs.add("-XstartOnFirstThread");
        }
        t.jvmArgs(jvmArgs);

        List<String> externalExtensions = null;

        Object externalConfigProperty = t.getProject().findProperty("externalRunConfiguration");
        if (externalConfigProperty instanceof String runConfigString) {
            File runConfigFile = new File(runConfigString);
            Gson gson = new Gson();
            try {
                String runConfig = ResourceGroovyMethods.getText(runConfigFile, "UTF-8");
                ExternalRunConfig runConfigObj = gson.fromJson(runConfig, ExternalRunConfig.class);
                externalExtensions = runConfigObj.runningExtensions;
                t.setDebug(true);
            } catch (IOException e) {
                throw new GradleException("Failed to read run configuration file: " + runConfigFile, e);
            }
        }

        List<String> finalExternalExtensions = externalExtensions;

        t.doFirst(new Action<Task>() {

            @Override
            public void execute(Task task) {
                File ldpath = typedExtractNativeArtifacts.get().getDestinationDirectory().get().getAsFile();
                List<HalSimPair> extensions = sim.getHalSimLocations(List.of(ldpath), getRunSimWithDebugJni().get());

                // Enumerate external extensions, launching the ones requested
                if (finalExternalExtensions != null) {
                    for (int i = 0; i < extensions.size(); i++) {
                        HalSimPair ext = extensions.get(i);
                        extensions.set(i, ext.withDefaultEnabled(finalExternalExtensions.contains(ext.libName)));
                    }
                }

                Map<String, String> env = sim.getEnvironment();

                t.environment(env);

                Optional<String> extensionString = extensions.stream().filter(x -> x.defaultEnabled).map(x -> x.libName)
                        .reduce((a, b) -> a + File.pathSeparator + b);
                if (extensionString.isPresent()) {
                    t.environment("HALSIM_EXTENSIONS", extensionString.get());
                }

                if (OperatingSystem.current().isWindows()) {
                    System.out.println(
                            "If you receive errors loading the JNI dependencies, make sure you have the latest Visual Studio C++ Redstributable installed.");
                    System.out.println(
                            "That can be found at https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads");
                }
            }
        });
    }

    private void configureExecutableNatives(JavaForkOptions t) {
        Task tt = (Task) t;

        tt.dependsOn(typedExtractNativeArtifacts);

        Provider<DirectoryProperty> destDir = project.getProviders().provider(() -> {
            return typedExtractNativeArtifacts.get().getDestinationDirectory();
        });

        tt.getInputs().dir(destDir);

        tt.doFirst(new TestTaskDoFirstAction(t, destDir));
    }

    public void configureTestTasks(Test t) {
        configureExecutableNatives(t);

        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/jdk.internal.vm=ALL-UNNAMED");
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/java.lang=ALL-UNNAMED");
        jvmArgs.add("--enable-native-access=ALL-UNNAMED");
        t.jvmArgs(jvmArgs);

        t.testLogging(new Action<TestLogging>() {
            @Override
            public void execute(TestLogging log) {
                log.events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_ERROR,
                        TestLogEvent.STANDARD_OUT);
                log.setShowStandardStreams(true);
            }
        });
    }

    public void configureApplication(JavaApplication application) {
        externalSimulationTask.configure(x -> x.setApplication(application));
        project.getTasks().named("run").configure(t -> {
            configureRunTask((JavaExec) t);
        });
    }

    @Inject
    public WPIJavaExtension(Project project, SimulationExtension sim, WPIVersionsExtension versions) {
        this.project = project;
        this.sim = sim;
        extractNativeDebugArtifacts = project.getTasks().register("extractDebugNative",
                ExtractNativeJavaArtifacts.class);
        extractNativeReleaseArtifacts = project.getTasks().register("extractReleaseNative",
                ExtractNativeJavaArtifacts.class);

        runSimWithDebugJni = project.getObjects().property(Boolean.class);
        runSimWithDebugJni.set(false);
        deps = project.getObjects().newInstance(WPIJavaDepsExtension.class, versions);
        vendor = project.getExtensions().getByType(WPIVendorDepsExtension.class).getJavaVendor();

        debugNativeConfiguration = project.getConfigurations().create("nativeDebug");
        releaseNativeConfiguration = project.getConfigurations().create("nativeRelease");

        PatternFilterable filterable = new PatternSet();
        filterable.include("**/*.so*", "**/*.dylib", "**/*.pdb", "**/*.dll");

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
        Callable<Set<File>> releaseCallable = () -> releaseView.getFiles().getAsFileTree().matching(filterable)
                .getFiles();

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

        typedExtractNativeArtifacts = project.getProviders().provider(() -> {
            if (runSimWithDebugJni.get()) {
                return extractNativeDebugArtifacts.get();
            } else {
                return extractNativeReleaseArtifacts.get();
            }
        });

        externalSimulationTask = project.getTasks().register("simulateExternalJava",
                JavaExternalSimulationTask.class, t -> {
                    t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("sim/java.json"));
                    t.setDependencies(typedExtractNativeArtifacts, runSimWithDebugJni);
                });

        project.getTasks().register("simulateJava", t -> {
            t.doLast(new Action<Task>() {
                @Override
                public void execute(Task arg0) {
                    throw new GradleException("The simulateJava task has been removed. Use run instead.");
                }
            });

        });
    }
}
