package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.deployutils.deploy.DeployExtension;

public class FRCExtension {
    private final Project project;

    @Inject
    public FRCExtension(Project project, DeployExtension deployExtension) {
        this.project = project;

        debugFileTask = project.getTasks().register("writeDebugInfo", DebugFileTask.class, t -> {
            t.getDebugFile().set(project.getLayout().getBuildDirectory().file("debug/debug_info.json"));
        });

        deployLogFile = project.getTasks().register("writeDeployFile", CreateLogFileTask.class, t -> {
            t.getDeployFile().set(project.getLayout().getBuildDirectory().file("debug/deploy.json"));
        });

        deployExtension.getDeployTask().configure(t -> {
            t.dependsOn(debugFileTask);
            t.dependsOn(deployLogFile);
        });
    }

    private final TaskProvider<DebugFileTask> debugFileTask;
    private final TaskProvider<CreateLogFileTask> deployLogFile;

    public TaskProvider<DebugFileTask> getDebugFileTask() {
        return debugFileTask;
    }

    public Project getProject() {
        return project;
    }

    public int getTeamOrDefault(int teamDefault) {
        if (project.hasProperty("teamNumber")) {
            return Integer.parseInt((String) project.findProperty("teamNumber"));
        }

        int number = getTeamNumberFromJSON();
        if (number < 0) {
            return teamDefault;
        }
        return number;
    }

    public int getTeamNumber() {
        if (project.hasProperty("teamNumber")) {
            return Integer.parseInt((String) project.findProperty("teamNumber"));
        }

        int number = getTeamNumberFromJSON();
        if (number < 0) {
            throw new TeamNumberNotFoundException();
        }
        return number;
    }

    public boolean getDebugOrDefault(boolean debugDefault) {
        if (project.hasProperty("debugMode")) {
            return true;
        }
        return debugDefault;
    }

    private static class PreferencesJson {
        public int teamNumber;
    }

    private int getTeamNumberFromJSON() {
        File jsonFile = project.getRootProject().file(".wpilib/wpilib_preferences.json");
        if (jsonFile.exists()) {
            Gson gson = new Gson();
            try {
                PreferencesJson json = gson.fromJson(ResourceGroovyMethods.getText(jsonFile), PreferencesJson.class);
                return json.teamNumber;
            } catch (JsonSyntaxException | IOException e) {
            }
        }
        return -1;
    }
}
