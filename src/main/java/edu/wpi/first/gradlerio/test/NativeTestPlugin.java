package edu.wpi.first.gradlerio.test;

import edu.wpi.first.toolchain.NativePlatforms;

import java.io.File;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.language.base.internal.ProjectLayout;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.nativeplatform.NativeComponentSpec;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.NativeExecutableSpec;
import org.gradle.nativeplatform.tasks.InstallExecutable;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;
import org.gradle.nativeplatform.test.NativeTestSuiteSpec;
import org.gradle.nativeplatform.test.googletest.GoogleTestTestSuiteBinarySpec;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.ComponentSpecContainer;
import org.gradle.platform.base.internal.BinarySpecInternal;

public class NativeTestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("simulateExternalCpp", NativeExternalSimulationTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Simulate External Task for native executable. Exports a JSON file for use by editors / tools");

            task.finalizedBy(project.getTasks().withType(ExternalSimulationMergeTask.class));
        });
    }

    public static class NativeTestRules extends RuleSource {
        @Mutate
        public void addBinaryFlags(BinaryContainer binaries) {
            binaries.withType(GoogleTestTestSuiteBinarySpec.class, bin -> {
                if (!bin.getTargetPlatform().getName().equals(NativePlatforms.desktop)) {
                    ((BinarySpecInternal)bin).setBuildable(false);
                }
                bin.getCppCompiler().define("RUNNING_FRC_TESTS");
                bin.getcCompiler().define("RUNNING_FRC_TESTS");
            });
        }

        @Mutate
        void addSimulationTasks(ModelMap<Task> tasks, ComponentSpecContainer components) {
            for (NativeExecutableSpec component : components.withType(NativeExecutableSpec.class)) {
                for (NativeExecutableBinarySpec bin : component.getBinaries().withType(NativeExecutableBinarySpec.class)) {
                    if (bin.getTargetPlatform().getOperatingSystem().isCurrent() && !bin.getTargetPlatform().getName().equals(NativePlatforms.roborio)) {
                        String name = "simulate" + StringGroovyMethods.capitalize((CharSequence)((BinarySpecInternal)bin).getProjectScopedName());
                        tasks.create(name, NativeSimulationTask.class, task -> {
                            task.setGroup("GradleRIO");
                            task.setDescription("Launch simulation for native component " + component.getName());
                            task.setBinary(bin);
                            task.dependsOn(bin.getTasks().getInstall());
                        });
                    }
                }
            }
        }

        @Validate
        public void populateExternalSimBinaries(ModelMap<Task> tasks, ComponentSpecContainer components, final BinaryContainer binaries, ExtensionContainer extCont, ProjectLayout projectLayout) {

            NativeExternalSimulationTask mainTask = (NativeExternalSimulationTask)tasks.get("simulateExternalCpp");
            Project project = (Project) projectLayout.getProjectIdentifier();

            for (NativeExecutableSpec spec : components.withType(NativeExecutableSpec.class)) {
                for (NativeExecutableBinarySpec bin : spec.getBinaries().withType(NativeExecutableBinarySpec.class)) {
                    if (bin.getTargetPlatform().getOperatingSystem().isCurrent() && bin.getTargetPlatform().getName().equals(NativePlatforms.desktop) && bin.getBuildType().getName().equals("debug")) {
                        mainTask.getExeBinaries().add(bin);
                        mainTask.dependsOn(bin.getTasks().getInstall());
                    }
                }
            }

            for (NativeTestSuiteBinarySpec bin : binaries.withType(NativeTestSuiteBinarySpec.class)) {
                if (bin.getTargetPlatform().getOperatingSystem().isCurrent() && bin.getTargetPlatform().getName().equals(NativePlatforms.desktop) && bin.getBuildType().getName().equals("debug")) {
                    mainTask.getTestBinaries().add(bin);
                    mainTask.dependsOn(bin.getTasks().getInstall());
                }
            }
        }
    }
}
