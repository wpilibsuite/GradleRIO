02 - Updating GradleRIO
====

## Step 1. Update the Gradle Version
First, you must update your Gradle version. At the bottom of your `build.gradle` file, you will see the following lines:
```gradle
task wrapper(type: Wrapper) {
    gradleVersion = '<version>'
}
```

Change `<version>` to be the latest Gradle version. This Gradle version is listed in the [README](../README.md) under `Upgrading`. 

Next, run `./gradlew wrapper`

## Step 2. Update GradleRIO
Edit your `build.gradle` file to change the GradleRIO version.
```gradle
plugins {
    // ... other plugins ...
    id "jaci.openrio.gradle.GradleRIO" version "<version>"
}
```

Change `<version>` to be the latest GradleRIO plugin version. This version is listed in the [README](../README.md) under `Upgrading`.

Next, run `./gradlew build` to download the update. Make sure you're connected to the internet.