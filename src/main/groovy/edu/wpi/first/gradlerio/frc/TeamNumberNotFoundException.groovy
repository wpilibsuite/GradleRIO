package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import org.gradle.api.GradleException

@CompileStatic
class TeamNumberNotFoundException extends GradleException {
    public TeamNumberNotFoundException() {
      super("Could not find team number. Make sure either one is passed in, or the team number is set in the wpilib_preferences.json file. You can also use getTeamOrDefault(number) to pass in a default team number instead of getTeamValue")
    }
}
