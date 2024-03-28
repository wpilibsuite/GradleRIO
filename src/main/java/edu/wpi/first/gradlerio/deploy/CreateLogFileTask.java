package edu.wpi.first.gradlerio.deploy;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import org.ajoberstar.grgit.Grgit;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.OutputFile;

import java.util.HashMap;
import java.io.IOException;
import java.io.File;
import java.lang.Runtime;
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
    private File deployFile;
    private String json;
    private String gitDirectory;

    @Inject
    public CreateLogFileTask() {
        HashMap<String, String> data = new HashMap<String, String>();
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson jsongen = builder.create();
        Grgit grgit;

        try {
            HashMap<String, Object> openargs = new HashMap<String, Object>();
            openargs.put("dir", gitDirectory);
            grgit = Grgit.open(openargs);

            try {
                data.put(DEPLOY_ITEMS[4], grgit.getResolve().toRevisionString("HEAD"));
            } catch (Exception e) {
                throw new GradleException("Couldn't get git hash", e);
            }

            try {
                data.put(DEPLOY_ITEMS[5], grgit.getResolve().toBranchName("HEAD"));
            } catch (Exception e) {
                throw new GradleException("Couldn't get git branch", e);
            }

            try {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("dirty", "-dirty");
                args.put("always", true);

                data.put(DEPLOY_ITEMS[6], grgit.describe(args));
            } catch (Exception e) {
                throw new GradleException("Couldn't get git description", e);
            }
        } catch (Exception e) {
        }

        try {
            data.put(DEPLOY_ITEMS[0], Runtime.getRuntime().exec("hostname").getOutputStream().toString().strip());
        } catch (IOException e) {
            throw new GradleException("Couldn't get hostname", e);
        }

        data.put(DEPLOY_ITEMS[1], System.getProperty("user.name"));
        data.put(DEPLOY_ITEMS[2], LocalDateTime.now().toString());
        data.put(DEPLOY_ITEMS[3], System.getProperty("user.dir"));

        json = jsongen.toJson(data);
    }

    @TaskAction
    public void execute() throws IOException {
        deployFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(deployFile, json);
    }

    public void setDeployFile(String path) {
      deployFile = new File(path);
    }

    public void setGitDirectory(String dir) {
        gitDirectory = dir;
    }

    @OutputFile
    public File getDeployFile() {
        return deployFile;
    };
}
