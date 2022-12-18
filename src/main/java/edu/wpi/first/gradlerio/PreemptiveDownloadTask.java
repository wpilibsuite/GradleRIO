package edu.wpi.first.gradlerio;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;


public abstract class PreemptiveDownloadTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getFiles();

    @TaskAction
    public void execute() {
    }
}
