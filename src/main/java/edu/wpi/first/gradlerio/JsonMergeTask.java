package edu.wpi.first.gradlerio;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class JsonMergeTask extends DefaultTask implements SingletonTask {

    private String singletonName = "jsonMerge";
    private final DirectoryProperty folder;
    private final RegularFileProperty out;

    @Internal
    @Override
    public String getSingletonName() {
        return singletonName;
    }

    public void setSingletonName(String singletonName) {
        this.singletonName = singletonName;
    }

    @OutputFile
    public RegularFileProperty getOut() {
        return out;
    }

    @InputDirectory
    public DirectoryProperty getFolder() {
        return folder;
    }

    @Inject
    public JsonMergeTask(ObjectFactory objects) {
        folder = objects.directoryProperty();
        out = objects.fileProperty();
    }

    @TaskAction
    public void merge() {
        File containerFolder = folder.get().getAsFile();
        File outfile = out.get().getAsFile();

        if (containerFolder.exists()) {
            File[] files = containerFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".json") && pathname.getAbsolutePath() != outfile.getAbsolutePath();
				}
            });
            JsonUtil.mergeArrays(Arrays.asList(files), outfile);
        }
    }
}
