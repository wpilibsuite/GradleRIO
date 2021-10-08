package edu.wpi.first.gradlerio;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;

import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.artifact.Artifact;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import edu.wpi.first.gradlerio.deploy.roborio.FRCJREArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.FRCJavaArtifact;
import edu.wpi.first.deployutils.log.ETLogger;

public class PreemptiveDownloadTask extends DefaultTask {

    @TaskAction
    public void execute() {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("DownloadAll");

        Project project = getProject();
        List<Configuration> configs = new ArrayList<>();

        for (RemoteTarget target : project.getExtensions().getByType(DeployExtension.class).getTargets()) {
            for (Artifact artifact : target.getArtifacts()) {
                if (artifact instanceof FRCJREArtifact) {
                    Configuration cfg = ((FRCJREArtifact)artifact).getConfiguration().get();
                    logger.info("Found JRE Configuration: " + cfg.getName());
                    configs.add(cfg);
                } else if (artifact instanceof FRCJavaArtifact) {
                    Configuration cfg = ((FRCJavaArtifact)artifact).getNativeZipArtifact().getConfiguration().get();
                    logger.info("Found Java Configuration: " + cfg.getName());
                    configs.add(cfg);
                }
            }
        }

        for (Configuration cfg : configs) {
            if (cfg.isCanBeResolved()) {
                logger.info("Resolving Deps Configuration: " + cfg.getName());
                cfg.getResolvedConfiguration().getFiles();
            } else {
                logger.info("Can't resolve: " + cfg.getName());
            }
        }
    }
}
