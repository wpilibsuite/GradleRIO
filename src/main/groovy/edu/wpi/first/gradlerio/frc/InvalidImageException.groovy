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
        if (this.parsable)
            return "RoboRIO Image invalid! RoboRIO: ${imageVersion}, allowed: [${this.allowedImageVersions.join(", ")}]"
        else
            return "Could not parse image version!"
    }
}
