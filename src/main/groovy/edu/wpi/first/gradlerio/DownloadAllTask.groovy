package edu.wpi.first.gradlerio

import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.TaskAction

class DownloadAllTask extends DefaultTask {

    @TaskAction
    void downloadAll() {
        def logger = ETLoggerFactory.INSTANCE.create("DownloadAll")

        project.configurations.each { Configuration conf ->
            // Skip configurations that cannot be resolved
            if (conf.canBeResolved) {
                println("Resolving: " + conf.getName())
                conf.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact art ->
                    art.file    // Needed to trigger download
                }
            } else {
                logger.info("Can't resolve: " + conf.getName())
            }
        }
    }

}
