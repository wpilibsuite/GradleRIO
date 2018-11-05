package edu.wpi.first.gradlerio

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.TaskAction

class DownloadAllTask extends DefaultTask {

    @TaskAction
    void downloadAll() {
        project.configurations.each { Configuration conf ->
            println("Resolving: " + conf.getName())
            conf.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact art ->
                art.file    // Needed to trigger download
            }
        }
    }

}
