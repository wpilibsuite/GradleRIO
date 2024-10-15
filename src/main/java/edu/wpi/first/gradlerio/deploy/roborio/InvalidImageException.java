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
            "\nRoboRIO image and GradleRIO versions are incompatible:" +
            "\n\tCurrent image version: " + imageVersion +
            "\n\tGradleRIO-compatible image versions: " + String.join(", ", allowedImageVersions) +
            "\nSee https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-3/imaging-your-roborio.html" +
            "for information about upgrading the RoboRIO image." +
            "\nSee https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html and" +
            "\nhttps://docs.wpilib.org/en/stable/docs/software/vscode-overview/importing-gradle-project.html" +
            "\nfor information about updating WPILib and GradleRIO.";
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

    public InvalidImageException(String imageFile) {
        super("Could not parse image version!\n/etc/natinst/share/scs_imagemetadata.ini contents:\n" + imageFile);
        allowedImageVersions = List.of();
        this.imageVersion = "";
    }

}
