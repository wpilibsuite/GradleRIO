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
                    "\nRoboRIO firmware and GradleRIO versions are incompatible:" +
                    "\n\tCurrent firmware version: ${imageVersion}" +
                    "\n\tGradleRIO-compatible versions: [${this.allowedImageVersions.join(", ")}]" +
                    "\nSee https://docs.wpilib.org/en/stable/docs/getting-started/getting-started-frc-control-system/imaging-your-roborio.html" +
                    "for information about upgrading RoboRIO firmware and/or GradleRIO."

        } else {
            return "Could not parse image version!"
        }
    }
}
