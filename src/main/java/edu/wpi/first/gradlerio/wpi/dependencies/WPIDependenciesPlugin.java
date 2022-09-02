package edu.wpi.first.gradlerio.wpi.dependencies;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import edu.wpi.first.gradlerio.PreemptiveDownloadTask;

public class WPIDependenciesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        TaskProvider<PreemptiveDownloadTask> lazyPreempt = project.getTasks().register("downloadDepsPreemptively", PreemptiveDownloadTask.class);

        project.getTasks().withType(Jar.class, jarTask -> {
            jarTask.dependsOn(lazyPreempt);
        });
    }
}
