package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Project;

public class FRCExtension {
    private final Project project;

    @Inject
    public FRCExtension(Project project) {
        this.project = project;
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
