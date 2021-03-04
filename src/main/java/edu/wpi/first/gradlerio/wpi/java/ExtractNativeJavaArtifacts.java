package edu.wpi.first.gradlerio.wpi.java;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public abstract class ExtractNativeJavaArtifacts extends DefaultTask {

    public static interface ExtractParameters extends WorkParameters {
        ConfigurableFileCollection getFiles();
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

    @InputFiles
    public abstract ConfigurableFileCollection getFiles();

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @Inject
    public abstract WorkerExecutor getWorkerExecuter();

    @TaskAction
    public void execute() {
        WorkQueue workQueue = getWorkerExecuter().noIsolation();

        workQueue.submit(ExtractFiles.class, parameters -> {
            parameters.getFiles().setFrom(getFiles());
            parameters.getDestinationDirectory().set(getDestinationDirectory());
        });
    }

}
