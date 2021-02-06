package edu.wpi.first.gradlerio.frc

class InvalidImageException extends RuntimeException {
    String imageVersion
    List<String> allowedImageVersions
    boolean parsable

    InvalidImageException(String imageVersion, List<String> allowedImageVersions) {
        this.imageVersion = imageVersion
        this.allowedImageVersions = allowedImageVersions
        this.parsable = true
    }

    InvalidImageException() {
        this.parsable = false
    }

    @Override
    String getMessage() {
        if (this.parsable) {
            return "Invalid RoboRIO Image Version!" +
                    "\nRoboRIO image and GradleRIO versions are incompatible:" +
                    "\n\tCurrent image version: ${imageVersion}" +
                    "\n\tGradleRIO-compatible image versions: [${this.allowedImageVersions.join(", ")}]" +
                    "\nSee https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-3/imaging-your-roborio.html" +
                    "for information about upgrading the RoboRIO image" +
                    "\nSee https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html and" +
                    "\nhttps://docs.wpilib.org/en/stable/docs/software/vscode-overview/importing-gradle-project.html" +
                    "\nfor information about updating WPILib and GradleRIO."

        } else {
            return "Could not parse image version!"
        }
    }
}
