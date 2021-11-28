package edu.wpi.first.gradlerio.wpi.dependencies;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import edu.wpi.first.gradlerio.PreemptiveDownloadTask;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import groovy.transform.CompileStatic;

public class WPIDependenciesPlugin implements Plugin<Project> {

    @CompileStatic
    public static class MissingJniDependencyException extends RuntimeException {
        private static final long serialVersionUID = -3526743142145446835L;
        private final String dependencyName;
        private final String classifier;
        private final WPIVendorDepsExtension.JniArtifact artifact;

        public String getDependencyName() {
            return dependencyName;
        }

        public String getClassifier() {
            return classifier;
        }

        public WPIVendorDepsExtension.JniArtifact getArtifact() {
            return artifact;
        }

        public MissingJniDependencyException(String name, String classifier, WPIVendorDepsExtension.JniArtifact artifact) {
            super("Cannot find jni dependency: " + name + " for classifier: " + classifier);
            this.dependencyName = name;
            this.classifier = classifier;
            this.artifact = artifact;
        }
    }

    @Override
    public void apply(Project project) {
        WPIExtension wpi = project.getExtensions().getByType(WPIExtension.class);
        wpi.getVendor().loadAll();

        TaskProvider<PreemptiveDownloadTask> lazyPreempt = project.getTasks().register("downloadDepsPreemptively", PreemptiveDownloadTask.class);

        project.getTasks().register("vendordep", VendorDepTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Install vendordep JSON file from URL or local wpilib folder");
        });

        project.getTasks().withType(Jar.class, jarTask -> {
            jarTask.dependsOn(lazyPreempt);
        });
    }
}
