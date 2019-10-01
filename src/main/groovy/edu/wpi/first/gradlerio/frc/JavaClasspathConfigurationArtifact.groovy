package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import javax.inject.Inject
import jaci.gradle.deploy.artifact.FileCollectionArtifact
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.ActionWrapper
import org.gradle.api.Action
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.util.PatternFilterable
import jaci.gradle.deploy.artifact.AbstractArtifact
import jaci.gradle.deploy.artifact.CacheableArtifact
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import jaci.gradle.PathUtils

import java.util.concurrent.Callable

@CompileStatic
class JavaClasspathConfigurationArtifact extends AbstractArtifact implements Callable<FileCollection>, CacheableArtifact {

    ListProperty<Configuration> configurations
    final Property<FileCollection> files
    private FileCollection resolvedFiles

    @Inject
    JavaClasspathConfigurationArtifact(String name, Project project) {
        super(name, project)
        files = project.objects.property(FileCollection)
        directory = PathUtils.combine(FRCPlugin.LIB_DEPLOY_DIR, 'java')
        onlyIf = { DeployContext ctx ->
            files.isPresent() && !files.get().empty && !files.get().files.empty
        }

        files.set(project.files(this as Callable<FileCollection>))
    }

    Object cache = "md5sum"
    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        if (files.isPresent()) {
            Map<String, File> filesToWrite = [:]
            files.get().files.each {
                def folderName = it.name.take(it.name.lastIndexOf('.')).toString()
                def key = "${folderName}/${it.name}".toString()
                filesToWrite.put(key, it)
            }
            context.put(filesToWrite, cacheResolver?.resolve(cache))
        } else {
            context.logger?.log("No file(s) provided for ${toString()}")
        }
    }

    List<String> getFileNames() {
        return files.get().files.collect { it.name.take(it.name.lastIndexOf('.')).toString() }
    }

    @Override
    FileCollection call() {
        if (resolvedFiles != null) {
            return resolvedFiles
        }
        resolvedFiles = []
        configurations.get.each() {
            // Only resolve files, not classpath folders
            def conf = configuration.resolvedConfiguration.files.findAll { !it.isDirectory() }
            resolvedFiles += project.files(conf)
        }
        
        return resolvedFiles
    }
}
