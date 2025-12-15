package org.wpilib.gradlerio.wpi.dependencies;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import org.wpilib.deployutils.deploy.DeployExtension;
import org.wpilib.deployutils.deploy.artifact.Artifact;
import org.wpilib.deployutils.deploy.target.RemoteTarget;
import org.wpilib.deployutils.log.ETLogger;
import org.wpilib.deployutils.log.ETLoggerFactory;
import org.wpilib.gradlerio.PreemptiveDownloadTask;
import org.wpilib.gradlerio.deploy.systemcore.FRCJavaArtifact;

public class WPIDependenciesPlugin implements Plugin<Project> {

    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        TaskProvider<PreemptiveDownloadTask> lazyPreempt = project.getTasks().register("downloadDepsPreemptively", PreemptiveDownloadTask.class);

        lazyPreempt.configure(x -> {
            Provider<Set<File>> filesProvider = project.provider(() -> {
                return getFiles();
            });
            x.getFiles().from(filesProvider);
        });

        project.getTasks().withType(Jar.class, jarTask -> {
            jarTask.dependsOn(lazyPreempt);
        });
    }

    private Set<File> getFiles() {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("DownloadAll");

        Set<File> files = new HashSet<>();

        List<Configuration> configs = new ArrayList<>();

        for (RemoteTarget target : project.getExtensions().getByType(DeployExtension.class).getTargets()) {
            for (Artifact artifact : target.getArtifacts()) {
                if (artifact instanceof FRCJavaArtifact) {
                    Configuration cfg = ((FRCJavaArtifact)artifact).getNativeZipArtifact().getConfiguration().get();
                    logger.info("Found Java Configuration: " + cfg.getName());
                    configs.add(cfg);
                }
            }
        }

        for (Configuration cfg : configs) {
            if (cfg.isCanBeResolved()) {
                logger.info("Resolving Deps Configuration: " + cfg.getName());
                files.addAll(cfg.getIncoming().getFiles().getFiles());
            } else {
                logger.info("Can't resolve: " + cfg.getName());
            }
        }

        return files;
    }
}
