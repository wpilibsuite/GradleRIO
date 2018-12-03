package edu.wpi.first.gradlerio.frc

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class FRCExtension {

    final Project project;

    FRCExtension(Project project) {
        this.project = project;
    }

    int getTeamOrDefault(int teamDefault) {
        if (project.hasProperty("teamNumber"))
            return Integer.parseInt(project.property("teamNumber") as String)

        def number = getTeamNumberFromJSON()
        if (number < 0)
            return teamDefault
        return number
    }

    int getTeamNumber() {
        if (project.hasProperty("teamNumber"))
            return Integer.parseInt(project.property("teamNumber") as String)
        def number = getTeamNumberFromJSON()
        if (number < 0)
            throw new TeamNumberNotFoundException()
        return number
    }

    boolean getDebugOrDefault(boolean debugDefault) {
        if (project.hasProperty("debugMode"))
            return true
        return debugDefault
    }

    private int getTeamNumberFromJSON() {
        def jsonFile = project.rootProject.file(".wpilib/wpilib_preferences.json")
        if (jsonFile.exists()) {
            def parsedJson = new JsonSlurper().parseText(jsonFile.text)
            def teamNumber = parsedJson['teamNumber']
            if (teamNumber != null)
                return teamNumber as Integer
        }
        return -1;
    }

}
