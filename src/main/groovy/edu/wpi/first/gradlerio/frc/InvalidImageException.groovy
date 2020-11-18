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
                    "\n\tCurrent Version: ${imageVersion}" +
                    "\n\tAllowed Versions: [${this.allowedImageVersions.join(", ")}]" +
                    "\nPlease image your RoboRIO with the latest firmware. " +
                    "For more info, see https://docs.wpilib.org/en/stable/docs/getting-started/getting-started-frc-control-system/imaging-your-roborio.html"

        } else {
            return "Could not parse image version!"
        }
    }
}
