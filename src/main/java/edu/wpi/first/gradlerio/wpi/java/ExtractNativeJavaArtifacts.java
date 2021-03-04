package edu.wpi.first.gradlerio.wpi.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import edu.wpi.first.gradlerio.wpi.WPIPlugin;

public abstract class ExtractNativeJavaArtifacts extends DefaultTask {

    public static interface ExtractParameters extends WorkParameters {
        List<File> getFiles();
        DirectoryProperty getDestinationDirectory();
    }

    public static abstract class ExtractFiles implements WorkAction<ExtractParameters> {
        @Inject
        public abstract FileSystemOperations getFileSystemOperations();

        @Override
        public void execute() {
            ExtractParameters parameters = getParameters();
            getFileSystemOperations().sync(copySpec -> {
                copySpec.into(parameters.getDestinationDirectory());
                copySpec.from(parameters.getFiles());
            });
        }
    }

    private final List<Configuration> configurations = new ArrayList<>();

    // This needs to take a configuration

    @InputFiles
    public List<Configuration> getConfigurations() {

    }

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @Inject
    public abstract WorkerExecutor getWorkerExecuter();

    @TaskAction
    public void execute() {
        WorkQueue workQueue = getWorkerExecuter().noIsolation();

        PatternFilterable filterable = new PatternSet();
        filterable.include("**/*.so", "**/*.dylib", "**/*.pdb", "**/*.dll");

        List<File> files = new ArrayList<>();

        for (Configuration configuration : configurations) {
            ArtifactView view = configuration.getIncoming().artifactView(viewConfiguration -> {
                viewConfiguration.attributes(attributeContainer -> {
                    attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                    WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
                });
            });

            files.addAll(view.getFiles().getAsFileTree().matching(filterable).getFiles());
        }

        workQueue.submit(ExtractFiles.class, parameters -> {
            parameters.getFiles().setFrom();
            parameters.getDestinationDirectory().set(getDestinationDirectory());
        });
    }

}
