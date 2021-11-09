package edu.wpi.first.gradlerio.wpi.cpp;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.simulation.NativeExternalSimulationTask;
import edu.wpi.first.gradlerio.simulation.NativeSimulationTask;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
import edu.wpi.first.nativeutils.NativeUtils;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.NativePlatforms;
import edu.wpi.first.toolchain.ToolchainExtension;
import edu.wpi.first.toolchain.ToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;

public class WPINativeExtension {
    private final WPINativeDepsExtension deps;
    private final WPINativeVendorDepsExtension vendor;

    public WPINativeDepsExtension getDeps() {
        return deps;
    }

    public WPINativeVendorDepsExtension getVendor() {
        return vendor;
    }

    private final TaskProvider<NativeExternalSimulationTask> releaseExternalSimulationTask;
    private final TaskProvider<NativeExternalSimulationTask> debugExternalSimulationTask;

    private final TaskProvider<NativeExternalSimulationTask> releaseExternalTestSimulationTask;
    private final TaskProvider<NativeExternalSimulationTask> debugExternalTestSimulationTask;

    public TaskProvider<NativeExternalSimulationTask> getExternalSimulationReleaseTask() {
        return releaseExternalSimulationTask;
    }

    public TaskProvider<NativeExternalSimulationTask> getExternalTestReleaseTask() {
        return releaseExternalTestSimulationTask;
    }

    public TaskProvider<NativeExternalSimulationTask> getExternalSimulationDebugTask() {
        return debugExternalSimulationTask;
    }

    public TaskProvider<NativeExternalSimulationTask> getExternalTestDebugTask() {
        return debugExternalTestSimulationTask;
    }

    public void enableExternalTasks(VariantComponentSpec component) {
        component.getBinaries().withType(NativeExecutableBinarySpec.class, this::enableExternalTasks);
        component.getBinaries().withType(NativeTestSuiteBinarySpec.class, this::enableExternalTasks);
    }

    public void enableExternalTasks(NativeExecutableBinarySpec binary) {
        if (binary.getTargetPlatform().getName().equals(NativePlatforms.desktop)) {
            if (binary.getBuildType().getName().contains("debug")) {
                debugExternalSimulationTask.configure(x -> x.getBinaries().add(binary));
                simulationTaskDebug.configure(x -> x.getBinaries().add(binary));
            } else {
                releaseExternalSimulationTask.configure(x -> x.getBinaries().add(binary));
                simulationTaskRelease.configure(x -> x.getBinaries().add(binary));
            }
        }
    }

    public void enableExternalTasks(NativeTestSuiteBinarySpec binary) {
        if (binary.getTargetPlatform().getName().equals(NativePlatforms.desktop)) {
            if (binary.getBuildType().getName().contains("debug")) {
                debugExternalTestSimulationTask.configure(x -> x.getBinaries().add(binary));
            } else {
                releaseExternalTestSimulationTask.configure(x -> x.getBinaries().add(binary));
            }
        }
    }

    private final TaskProvider<NativeSimulationTask> simulationTaskRelease;
    private final TaskProvider<NativeSimulationTask> simulationTaskDebug;

    private final Property<Boolean> debugSimulation;

    public Property<Boolean> getDebugSimulation() {
        return debugSimulation;
    }

    @Inject
    public WPINativeExtension(Project project, WPIVendorDepsExtension vendorDeps, WPIVersionsExtension versions) {
        project.getPluginManager().apply(ToolchainPlugin.class);
        project.getPluginManager().apply(RoboRioToolchainPlugin.class);
        project.getPluginManager().apply(NativeUtils.class);
        project.getPluginManager().apply(WPINativeCompileRules.class);

        debugSimulation = project.getObjects().property(Boolean.class);
        debugSimulation.set(false);

        NativeUtilsExtension nte = project.getExtensions().getByType(NativeUtilsExtension.class);
        nte.withRoboRIO();
        nte.addWpiNativeUtils();

        deps = project.getObjects().newInstance(WPINativeDepsExtension.class, nte);

        vendor = project.getObjects().newInstance(WPINativeVendorDepsExtension.class, vendorDeps, nte, project);

        ToolchainExtension te = project.getExtensions().getByType(ToolchainExtension.class);
        te.getCrossCompilers().named(nte.getWpi().platforms.roborio, c -> {
            c.getOptional().set(false);
        });

        nte.getWpi().addWarnings();
        //nte.setSinglePrintPerPlatform();

        nte.getWpi().configureDependencies(wpiDeps -> {
            wpiDeps.getWpiVersion().set(versions.getWpilibVersion());
            wpiDeps.getNiLibVersion().set(versions.getNiLibrariesVersion());
            wpiDeps.getOpencvVersion().set(versions.getOpencvVersion());
            wpiDeps.getGoogleTestVersion().set(versions.getGoogleTestVersion());
            wpiDeps.getImguiVersion().set(versions.getImguiVersion());
            wpiDeps.getWpimathVersion().set(versions.getWpimathVersion());
        });

        simulationTaskRelease = project.getTasks().register("simulateNativeRelease", NativeSimulationTask.class);
        simulationTaskDebug = project.getTasks().register("simulateNativeDebug", NativeSimulationTask.class);

        project.getTasks().register("simulateNative", t -> {
            var simTask = project.getProviders().provider(() -> {
                if (getDebugSimulation().get()) {
                    return simulationTaskDebug;
                } else {
                    return simulationTaskRelease;
                }
            });
            t.dependsOn(simTask);
        });

        releaseExternalSimulationTask = project.getTasks().register("simulateExternalNativeRelease", NativeExternalSimulationTask.class, t -> {
            t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("sim/release_native.json"));
        });

        debugExternalSimulationTask = project.getTasks().register("simulateExternalNativeDebug", NativeExternalSimulationTask.class, t -> {
            t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("sim/debug_native.json"));
        });

        releaseExternalTestSimulationTask = project.getTasks().register("testExternalNativeRelease", NativeExternalSimulationTask.class, t -> {
            t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("test/release_native.json"));
        });

        debugExternalTestSimulationTask = project.getTasks().register("testExternalNativeDebug", NativeExternalSimulationTask.class, t -> {
            t.getSimulationFile().set(project.getLayout().getBuildDirectory().file("test/debug_native.json"));
        });
    }
}
