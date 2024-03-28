package edu.wpi.first.gradlerio.deploy;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import org.ajoberstar.grgit.Grgit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
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

    @TaskAction
    public void execute() {
        Gson builder = new GsonBuilder().create();
        Grgit grgit;
        HashMap<String, String> data = new HashMap<String, String>();

        try {
            grgit = Grgit.open();

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
        } catch (Exception e) {}

        try {
            data.put(DEPLOY_ITEMS[0], Runtime.getRuntime().exec("hostname").getOutputStream().toString().strip());
        } catch (IOException e) {
            throw new GradleException("Couldn't get hostname", e);
        }

        data.put(DEPLOY_ITEMS[1], System.getProperty("user.name"));
        data.put(DEPLOY_ITEMS[2], LocalDateTime.now().toString());
        data.put(DEPLOY_ITEMS[3], System.getProperty("user.dir"));

        deployFile = new File(builder.toJson(data));
    }

    @OutputFile
    public RegularFileProperty getDeployFile() {
        // TODO! figure out how to do this
        return deployFile;
    };
}
