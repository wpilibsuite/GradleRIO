# GradleRIO
GradleRIO is a powerful Gradle Plugin that allows teams competing in the FIRST
robotics competition to produce and build their code.

![](img/tty.gif)

GradleRIO works with Java and C++ (and others!), on Windows, Mac and Linux. GradleRIO automatically fetches WPILib, CTRE Toolsuite (Talon SRX) and NavX libraries, and you can even add your own libraries!

GradleRIO will not only deploy to your RoboRIO, but also to Coprocessors like the Raspberry Pi, Jetson and Pine64. You can even deploy to multiple targets at the same time!

GradleRIO will work with Eclipse or IntelliJ IDEA (for Java), and CLion or Visual Studio (for C++). Don't worry, you don't need an IDE if you don't want one, you can use Visual Studio Code, Notepad++, Sublime Text, Vim, or whatever you want, since all builds are done from the command line.

## Commands
Windows Users: It is recommended to use Powershell instead of CMD.
- ```./gradlew build``` will build your Robot Code
- ```./gradlew deploy``` will build and deploy your code.
- ```./gradlew riolog``` will display the RoboRIO console output on your computer (run with `-Pfakeds` if you don't have a driverstation connected).

- ```./gradlew smartDashboard``` will launch Smart Dashboard
- ```./gradlew shuffleboard``` will launch Shuffleboard, the 2018 replacement for SmartDashboard.
- ```./gradlew installToolchain``` will install the C++ Toolchains for your system (required for C++).

**At Competition? Connected to the Robot?** Run with the `--offline` flag. e.g. `./gradlew build deploy --offline`

## IDE Commands
- ```./gradlew idea``` will generate IDE files for IntelliJ IDEA (java)
- ```./gradlew eclipse``` will generate IDE files for Eclipse (java)
- ```./gradlew <component>VisualStudio``` will generate IDE files for the C/C++ component named `<component>` for Visual Studio (C++)
- ```./gradlew clion``` will generate IDE files for Clion (C++). Be warned that Clion support is hacky as Clion does not natively support Gradle.

## Misc Commands
- ```./gradlew telemetry``` will display the telemetry that GradleRIO reports. This can be disabled. See more about this [here](PRIVACY.md)

## Getting Started
Read the docs [here](docs/) to get started. They are much more descriptive than this README.

## Download
To get GradleRIO, download the [Quickstart Zip](Quickstart.zip) and unzip it to your project directory.
Please note that your java files must be in `src/main/java`, not just `src/`. C++ files are in `src/cpp` and `src/include`.

**C++ Users**: Run `./gradlew installToolchain` in order to install the FRC Toolchain.
**Java Users**: The Java installation will be automatically deployed to your RoboRIO. You do not need to use the Java Installer.

## Upgrading
To upgrade your version of GradleRIO, you must first upgrade gradle. Near the bottom of your build.gradle, change the wrapper version to the following, and then run `./gradlew wrapper`:
```gradle
task wrapper(type: Wrapper) {
    gradleVersion = '4.4'
}
```

Next, replace the version in the plugin line (only change the GradleRIO line):
```gradle
plugins {
    // ... other plugins ...
    id "jaci.openrio.gradle.GradleRIO" version "2018.02.17"
}
```

## Implementation Details
NOTE: This section is for advanced users. View the quickstart and examples for your language to get started.

### Full Spec
```gradle
deploy {
    targets {
        target('roborio', jaci.openrio.gradle.frc.RoboRIO) {
            team = 5333
            // Other values can be edited through EmbeddedTools.
            // See https://github.com/JacisNonsense/EmbeddedTools#spec
        }
        // Other targets can be edited through EmbeddedTools.
        // See https://github.com/JacisNonsense/EmbeddedTools#spec
    }
    artifacts {
        // Setup a Java Artifact. Required for Java Users.
        artifact('myJava', jaci.openrio.gradle.frc.FRCJavaArtifact) {
            targets << 'roborio'

            jvmArgs << '-Xmx=128m'      // Set more JVM Arguments. Optional.
            arguments << 'myCustomArgs' // The command-line arguments to launch your jar with. Optional.
            debug = true                // Enable to enable java debugging on the RoboRIO. Default: false
            debugPort = 8348            // Set the debugging port. Default: 8348
            robotCommand = './myOtherProgram'       // Set the contents of robotCommand. Optional, usually created depending on above values.

            // Other values can be edited through EmbeddedTools.
            // See https://github.com/JacisNonsense/EmbeddedTools#spec
        }

        // Setup a C++ (Native) Artifact. Required for C++ (Native) Users
        artifact('myNative', jaci.openrio.gradle.frc.FRCNativeArtifact) {
            targets << 'roborio'
            component = 'myFrcBinary'   // The name of the component you wish to build (required).

            arguments << 'myCustomArgs' // The command-line arguments to launch your jar with. Optional.
            debug = true                // Enable to enable java debugging on the RoboRIO. Default: false
            debugPort = 8348            // Set the debugging port. Default: 8348
            robotCommand = './myOtherProgram'       // Set the contents of robotCommand. Optional, usually created depending on above values.

            // Other values can be edited through EmbeddedTools.
            // See https://github.com/JacisNonsense/EmbeddedTools#spec
        }
        // Other artifacts can be edited through EmbeddedTools.
        // See https://github.com/JacisNonsense/EmbeddedTools#spec
    }
}

// Set the versions of libraries to use. This is calculated for you based
// off known-stable versions for the current year, but you can modify
// them here if you so desire. This block is not required.
wpi {
    wpilibVersion = '...'
    wpilibVersion = '...'
    opencvVersion = '...'
    wpilibVersion = '...'
    wpilibVersion = '...'

    ctreVersion = '...'
    ctreLegacyVersion = '...'   // NOTE: Legacy Toolsuite
    navxVersion = '...'

    smartDashboardVersion = '...'
    shuffleboardVersion = '...'

    toolchainVersion = '...'
}

// Set the dependencies you want to use in your JAVA project.
// WPILib adds WPILibJ, NTCore, OpenCV, CSCore among others.
// CTRE adds the CTRE Toolsuite (i.e. Talon SRX)
// NavX adds the NavX IMU library.
dependencies {
    compile wpilib()
    compile navx()
    compile ctre()
    // compile ctreLegacy() // NOTE: Legacy Toolsuite. Use above ctre() if you're not sure.
}

// Java only. Setup your Jar File.
jar {
    // Compile a 'fat jar' (libraries included)
    from configurations.compile.collect { it.isDirectory() ? it : zipTree(it) }
    // Include your Manifest. Arguments are your Robot Main Class.
    manifest jaci.openrio.gradle.GradleRIOPlugin.javaManifest('test.myClass')
}

// Set up your Native (C++) projects. Not needed in Java.
model {
    components {
        myFrcBinary(NativeExecutableSpec) {
            targetPlatform 'roborio'
            sources.cpp {
                source {
                    srcDir 'src/main/cpp'
                }
                // Add the libraries you wish to use in your NATIVE project.
                // WPILib adds WPILibJ, NTCore, OpenCV, CSCore among others.
                // CTRE adds the CTRE Toolsuite (i.e. Talon SRX)
                // NavX adds the NavX IMU library.
                lib library: "wpilib"
                lib library: "navx"
                lib library: "ctre"
                // lib library: "ctre_legacy"   // NOTE: Legacy Toolsuite. Use above ctre() if you're not sure.
            }
        }
    }
}

// GradleRIO reports some anonymous data to aid in future development
// This includes your gradle version, dependencies, plugins and versions.
// It does NOT include your IP address, username, password, or other confidential
// data.
// You can choose to disable this telemetry if you're paranoid.
// Telemetry is, by default, enabled.
telemetry {
    reportTelemetry = false
}
```
