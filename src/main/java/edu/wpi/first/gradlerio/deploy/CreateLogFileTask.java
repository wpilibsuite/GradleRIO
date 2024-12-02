package edu.wpi.first.gradlerio.deploy;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.OutputFile;

import java.util.HashMap;

import javax.inject.Inject;

import java.io.IOException;
import java.net.InetAddress;
import java.io.File;
import java.time.LocalDateTime;

public class CreateLogFileTask extends DefaultTask {
    public static final String[] DEPLOY_ITEMS = {
            "deployHost",
            "deployUser",
            "deployDate",
            "codePath",
            "gitHash",
            "gitBranch",
            "gitDesc",
    };
    private RegularFileProperty deployFile;
    private String json;
    private String gitDirectory;

    @Inject
    public CreateLogFileTask(Project project) {
      deployFile = project.getObjects().fileProperty();
    }

    @TaskAction
    public void execute() throws IOException {
        deployFile.getAsFile().get().getParentFile().mkdirs();
        HashMap<String, String> data = new HashMap<String, String>();
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson jsongen = builder.create();

        try {
          Repository repository = new FileRepositoryBuilder().setGitDir(new File(gitDirectory))
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build();

            try {
                data.put(DEPLOY_ITEMS[4], repository.resolve("HEAD").toString());
            } catch (Exception e) {
                getLogger().log(LogLevel.WARN, "Couldn't get git hash");
            }

            try {
                data.put(DEPLOY_ITEMS[5], repository.getBranch());
            } catch (Exception e) {
                getLogger().log(LogLevel.WARN, "Couldn't get git branch");
            }
            try {
                data.put(DEPLOY_ITEMS[6], repository.getGitwebDescription());
            } catch (Exception e) {
                getLogger().log(LogLevel.WARN, "Couldn't get git description");
            }
        } catch (Exception e) {
          getLogger().log(LogLevel.WARN, "Couldn't find git");
        }

        try {
            data.put(DEPLOY_ITEMS[0], InetAddress.getLocalHost().getHostName());
        } catch (IOException e) {
            getLogger().log(LogLevel.WARN, "Couldn't get host name");
        }

        data.put(DEPLOY_ITEMS[1], System.getProperty("user.name"));
        data.put(DEPLOY_ITEMS[2], LocalDateTime.now().toString());
        data.put(DEPLOY_ITEMS[3], System.getProperty("user.dir"));

        json = jsongen.toJson(data);
        ResourceGroovyMethods.setText(deployFile.getAsFile().get(), json);
    }

    public void setGitDirectory(String dir) {
        gitDirectory = dir;
    }

    @OutputFile
    public RegularFileProperty getDeployFile() {
        return deployFile;
    };
}
