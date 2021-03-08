package edu.wpi.first.gradlerio.deploy.roborio;

import java.util.ArrayList;
import java.util.List;

public class InvalidImageException extends RuntimeException {
    private static final long serialVersionUID = 1840472963312724922L;
    private final String imageVersion;
    private final List<String> allowedImageVersions;

    public String getImageVersion() {
        return imageVersion;
    }

    public List<String> getAllowedImageVersions() {
        return allowedImageVersions;
    }

    private static String parseMessage(String imageVersion, List<String> allowedImageVersions) {
        return "Invalid RoboRIO Image Version!" +
            "\nRoboRIO firmware and GradleRIO versions are incompatible:" +
            "\n\tCurrent firmware version: " + imageVersion +
            "\n\tGradleRIO-compatible versions: " + String.join(", ", allowedImageVersions) +
            "\nSee https://docs.wpilib.org/en/stable/docs/getting-started/getting-started-frc-control-system/imaging-your-roborio.html" +
            "for information about upgrading RoboRIO firmware and/or GradleRIO.";
    }

    public InvalidImageException(String imageVersion, List<String> allowedImageVersions) {
        super(parseMessage(imageVersion, allowedImageVersions));
        this.imageVersion = imageVersion;
        this.allowedImageVersions = new ArrayList<>(allowedImageVersions);
    }

    public InvalidImageException() {
        super("Could not parse image version!");
        allowedImageVersions = List.of();
        this.imageVersion = "";
    }


}
