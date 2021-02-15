package edu.wpi.first.gradlerio;

import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;

public class DownloadAllTask extends DefaultTask {

    @TaskAction
    public void downloadAll() {
        ETLogger logger = ETLoggerFactory.INSTANCE.create("DownloadAll");

        for(Configuration conf : getProject().getConfigurations()) {
            // Skip configurations that cannot be resolved
            if (conf.isCanBeResolved()) {
                System.out.println("Resolving: " + conf.getName());
                for (ResolvedArtifact art : conf.getResolvedConfiguration().getResolvedArtifacts()) {
                    art.getFile();    // Needed to trigger download
                }
            } else {
                logger.info("Can't resolve: " + conf.getName());
            }
        }
    }

}
