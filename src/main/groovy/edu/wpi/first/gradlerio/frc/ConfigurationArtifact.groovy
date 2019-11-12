package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import javax.inject.Inject
import jaci.gradle.deploy.artifact.FileCollectionArtifact
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.ActionWrapper
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.util.PatternFilterable

import java.util.concurrent.Callable

@CompileStatic
class ConfigurationArtifact extends FileCollectionArtifact implements Callable<FileCollection> {

    Configuration configuration
    boolean zipped
    Action<PatternFilterable> filter

    Set<File> configFileCaches

    @Inject
    ConfigurationArtifact(String name, Project project) {
        super(name, project)
        directory = FRCPlugin.LIB_DEPLOY_DIR
        onlyIf = { DeployContext ctx ->
            files.isPresent() && !files.get().empty && !files.get().files.empty
        }

        postdeploy << new ActionWrapper({ DeployContext ctx ->
            FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR)
            ctx.execute("ldconfig")
        })

        files.set(project.files(this as Callable<FileCollection>))
    }

    @Override
    FileCollection call() {
        if (configFileCaches == null) {
            configFileCaches = configuration.resolvedConfiguration.files
        }
        if (zipped) {
            def allfiles = configFileCaches.collect { File file ->
                project.zipTree(file).matching(filter)
            }.findAll { it != null }

            return allfiles.empty ? project.files() : allfiles.inject { a, b -> a + b }
        } else {
            return project.files(configFileCaches)
        }
    }
}
