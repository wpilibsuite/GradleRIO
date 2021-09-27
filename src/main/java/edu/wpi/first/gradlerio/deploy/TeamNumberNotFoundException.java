package edu.wpi.first.gradlerio.deploy;

public class TeamNumberNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -7735297736598850111L;

    public TeamNumberNotFoundException() {
        super("Could not find team number. Make sure either one is passed in, or the team number is set in the wpilib_preferences.json file. You can also use getTeamOrDefault(number) to pass in a default team number instead of getTeamValue");
    }
}
