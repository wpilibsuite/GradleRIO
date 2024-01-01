package edu.wpi.first.gradlerio.wpi.java;

import edu.wpi.first.gradlerio.simulation.HalSimPair;
import edu.wpi.first.gradlerio.simulation.JavaExternalSimulationTask;
import edu.wpi.first.gradlerio.simulation.JavaSimulationTask;
import edu.wpi.first.gradlerio.wpi.WPIPlugin;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;
import edu.wpi.first.nativeutils.vendordeps.WPIJavaVendorDepsExtension;
import edu.wpi.first.nativeutils.vendordeps.WPIVendorDepsExtension;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLogging;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.jvm.tasks.Jar;
import org.gradle.process.JavaForkOptions;

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

  private final Property<Boolean> debugJni;

  public Property<Boolean> getDebugJni() {
    return debugJni;
  }

  private final TaskProvider<JavaExternalSimulationTask> externalSimulationTaskDebug;

  public TaskProvider<JavaExternalSimulationTask> getExternalSimulationTaskDebug() {
    return externalSimulationTaskDebug;
  }

  private final TaskProvider<JavaExternalSimulationTask> externalSimulationTaskRelease;

  public TaskProvider<JavaExternalSimulationTask> getExternalSimulationTaskRelease() {
    return externalSimulationTaskRelease;
  }

  private final TaskProvider<JavaSimulationTask> simulationTaskDebug;
  private final TaskProvider<JavaSimulationTask> simulationTaskRelease;

  public TaskProvider<JavaSimulationTask> getSimulationTaskDebug() {
    return simulationTaskDebug;
  }

  public TaskProvider<JavaSimulationTask> getSimulationTaskRelease() {
    return simulationTaskRelease;
  }

  private void configureSimulationTask(
      JavaSimulationTask t, boolean debug, Provider<ExtractNativeJavaArtifacts> extract) {
    configureExecutableNatives(t, extract);
    if (OperatingSystem.current().isMacOsX()) {
      t.jvmArgs("-XstartOnFirstThread");
    }

    t.doFirst(
        new Action<Task>() {

          @Override
          public void execute(Task task) {
            File ldpath = extract.get().getDestinationDirectory().get().getAsFile();
            List<HalSimPair> extensions = sim.getHalSimLocations(List.of(ldpath), debug);
            Map<String, String> env = sim.getEnvironment();

            t.environment(env);

            Optional<String> extensionString =
                extensions.stream()
                    .filter(x -> x.defaultEnabled)
                    .map(x -> x.libName)
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

  private void configureExecutableNatives(
      JavaForkOptions t, Provider<ExtractNativeJavaArtifacts> extract) {
    Task tt = (Task) t;

    tt.dependsOn(extract);

    Provider<DirectoryProperty> destDir =
        project
            .getProviders()
            .provider(
                () -> {
                  return extract.get().getDestinationDirectory();
                });

    tt.getInputs().dir(destDir);

    tt.doFirst(new TestTaskDoFirstAction(t, destDir));
  }

  public void configureTestTasks(Test t) {
    Property<Boolean> debug = this.getDebugJni();
    TaskProvider<ExtractNativeJavaArtifacts> debugTask = this.getExtractNativeDebugArtifacts();
    TaskProvider<ExtractNativeJavaArtifacts> releaseTask = this.getExtractNativeReleaseArtifacts();
    Provider<ExtractNativeJavaArtifacts> extract =
        project
            .getProviders()
            .provider(
                () -> {
                  if (debug.get()) {
                    return debugTask.get();
                  } else {
                    return releaseTask.get();
                  }
                });

    configureExecutableNatives(t, extract);

    t.testLogging(
        new Action<TestLogging>() {
          @Override
          public void execute(TestLogging log) {
            log.events(
                TestLogEvent.FAILED,
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_ERROR,
                TestLogEvent.STANDARD_OUT);
            log.setShowStandardStreams(true);
          }
        });
  }

  public void configureExecutableTasks(Jar jar) {
    externalSimulationTaskDebug.configure(x -> x.getJars().add(jar));
    externalSimulationTaskRelease.configure(x -> x.getJars().add(jar));
    simulationTaskDebug.configure(x -> x.classpath(jar));
    simulationTaskRelease.configure(x -> x.classpath(jar));
  }

  @Inject
  public WPIJavaExtension(Project project, SimulationExtension sim, WPIVersionsExtension versions) {
    this.project = project;
    this.sim = sim;
    extractNativeDebugArtifacts =
        project.getTasks().register("extractDebugNative", ExtractNativeJavaArtifacts.class);
    extractNativeReleaseArtifacts =
        project.getTasks().register("extractReleaseNative", ExtractNativeJavaArtifacts.class);

    debugJni = project.getObjects().property(Boolean.class);
    debugJni.set(false);
    deps = project.getObjects().newInstance(WPIJavaDepsExtension.class, versions);
    vendor = project.getExtensions().getByType(WPIVendorDepsExtension.class).getJavaVendor();

    debugNativeConfiguration = project.getConfigurations().create("nativeDebug");
    releaseNativeConfiguration = project.getConfigurations().create("nativeRelease");

    PatternFilterable filterable = new PatternSet();
    filterable.include("**/*.so*", "**/*.dylib", "**/*.pdb", "**/*.dll");

    ArtifactView debugView =
        debugNativeConfiguration
            .getIncoming()
            .artifactView(
                viewConfiguration -> {
                  viewConfiguration.attributes(
                      attributeContainer -> {
                        attributeContainer.attribute(
                            WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                            WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
                      });
                });

    ArtifactView releaseView =
        releaseNativeConfiguration
            .getIncoming()
            .artifactView(
                viewConfiguration -> {
                  viewConfiguration.attributes(
                      attributeContainer -> {
                        attributeContainer.attribute(
                            WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                            WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
                      });
                });

    Callable<Set<File>> debugCallable =
        () -> debugView.getFiles().getAsFileTree().matching(filterable).getFiles();
    Callable<Set<File>> releaseCallable =
        () -> releaseView.getFiles().getAsFileTree().matching(filterable).getFiles();

    debugFileCollection = project.files(debugCallable);
    releaseFileCollection = project.files(releaseCallable);

    extractNativeDebugArtifacts.configure(
        extract -> {
          extract
              .getDestinationDirectory()
              .set(project.getLayout().getBuildDirectory().dir("jni/debug"));
          extract.getFiles().from(sim.getDebugFileCollection());
          extract.getFiles().from(debugFileCollection);
        });

    extractNativeReleaseArtifacts.configure(
        extract -> {
          extract
              .getDestinationDirectory()
              .set(project.getLayout().getBuildDirectory().dir("jni/release"));
          extract.getFiles().from(sim.getReleaseFileCollection());
          extract.getFiles().from(releaseFileCollection);
        });

    externalSimulationTaskDebug =
        project
            .getTasks()
            .register(
                "simulateExternalJavaDebug",
                JavaExternalSimulationTask.class,
                t -> {
                  t.getSimulationFile()
                      .set(project.getLayout().getBuildDirectory().file("sim/debug_java.json"));
                  t.setDependencies(sim, extractNativeDebugArtifacts, true, project);
                });

    externalSimulationTaskRelease =
        project
            .getTasks()
            .register(
                "simulateExternalJavaRelease",
                JavaExternalSimulationTask.class,
                t -> {
                  t.getSimulationFile()
                      .set(project.getLayout().getBuildDirectory().file("sim/release_java.json"));
                  t.setDependencies(sim, extractNativeReleaseArtifacts, false, project);
                });

    simulationTaskDebug =
        project
            .getTasks()
            .register(
                "simulateJavaDebug",
                JavaSimulationTask.class,
                t -> {
                  configureSimulationTask(t, true, extractNativeDebugArtifacts);
                });

    simulationTaskRelease =
        project
            .getTasks()
            .register(
                "simulateJavaRelease",
                JavaSimulationTask.class,
                t -> {
                  configureSimulationTask(t, false, extractNativeReleaseArtifacts);
                });

    project
        .getTasks()
        .register(
            "simulateJava",
            t -> {
              var simTask =
                  project
                      .getProviders()
                      .provider(
                          () -> {
                            if (getDebugJni().get()) {
                              return simulationTaskDebug;
                            } else {
                              return simulationTaskRelease;
                            }
                          });
              t.dependsOn(simTask);
            });
  }
}
