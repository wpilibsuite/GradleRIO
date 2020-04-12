package edu.wpi.first.gradlerio.frcvision

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
            return "FRCVision Image invalid! Pi: ${imageVersion}, allowed: [${this.allowedImageVersions.join(", ")}]"
        else
            return "Could not parse image version!"
    }
}
