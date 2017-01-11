# GradleRIO
GradleRIO is a powerful Gradle Plugin that allows teams competing in the FIRST
robotics competition to produce and build their code without being limited to
the Eclipse IDE.

GradleRIO extracts the WPILib sources from the eclipse plugin and allows you to
use it with Eclipse, IntelliJ IDEA or any IDE of your choice. GradleRIO also allows you to build and
deploy your code to the RoboRIO from the command-line, or through the IDE.

## Basic Commands
- ```gradlew idea``` / ```gradlew eclipse``` will generate all the necessary files for your Development Environment workspace
- ```gradlew build``` will build your Robot Code
- ```gradlew build deploy``` will build and deploy your code to the RoboRIO

## Features
### Dependency Management
GradleRIO manages dependencies for your robot program. From things like WPILib, to Talon SRX Libraries, to libraries you've written yourself. This is all done in the `dependencies {}` block of your `build.gradle`. See the [examples](examples/) for more details

### IDE Support
GradleRIO supports a multitude of IDEs, with Eclipse and IntelliJ IDEA being supported by default. It's as simple as running ```gradlew eclipse``` or ```gradlew idea``` to get your Development Environment.

### Robot Deployment
GradleRIO is flexible with your Robot. When deploying code, GradleRIO will search for your RoboRIO on USB and Network Interfaces. If your RoboRIO is running on a Network that is not your FRC-Provided Radio (perhaps wired ethernet), you can even specify an IP Address to deploy to. 

Because all of the deployment properties are stored in ```build.gradle```, if you decide to change Team Number or Robot Base Class, it's very simple to update this info and have deployment work without a hitch. 

### Extensibility
GradleRIO runs on an underlying Gradle Build System, which is extremely extensible and is used by many professional software companies around the world. Gradle is stable, fast and flexible, with a huge community to back it. You can even write your own Plugins for Gradle if you really want to take control.

IntelliJ IDEA and the latest releases of Eclipse have Gradle support built in, allowing you to run all these commands without leaving your IDE. Alternatively, you can also use the Command Line to build and deploy your code, and is easily automatable.

## Download
To get GradleRIO, download the [Quickstart Zip](Quickstart.zip) and unzip it to your project directory. Update the version in the `build.gradle` file with the [latest plugin version](https://plugins.gradle.org/plugin/jaci.openrio.gradle.GradleRIO) and you're good to go!

## Implementation Details
### Full Spec
```gradle
frc {
    team = "5333"                         // Your team number. Required
    robotClass = "org.usfirst.myteam"     // Your robot main class (where you implement RobotBase / IterativeRobot)
    
    deployTimeout = 3                     // The time (in seconds) to wait before timing out on an SSH connection
    deployDirectory = "/home/lvuser"      // The directory to deploy your jar to
    rioIP = "10.53.33.2"                  // The IP Address of your RoboRIO. Automatically calculated from team number if not set
    rioHost = "my_roborio.local"          // The Hostname of your RoboRIO. Automatically calculated from team number if not set

    robotCommand = "./something_else"     // The command to run when starting your robot program. This is calculated by default, with runArguments
    runArguments = "--hello"              // The command-line arguments to launch your jar with. By default, there are none. Not used if robotCommand set
    useDebugCommand = true                // Set to true if you want to use a remote debugger with your robot program. Not used if robotCommand is set.
}

wpi {
    wpilibVersion = "+"                   // The WPILib version to use. For this version of GradleRIO, must be a 2017 version
    ntcoreVersion = "+"                   // The NetworkTables Core version to use.
    opencvVersion = "+"                   // The OpenCV version to use
    cscoreVersion = "+"                   // The CSCore version to use

    talonSrxVersion = "+"                 // The CTRE Toolsuite (Talon SRX) version to use.
}

dependencies {
    compile wpilib()                    // Compile with WPILib and it's dependencies (ntcore, opencv, cscore)
    compile talonSrx()                  // Compile with the Talon SRX Library

    // Use these to link your own third-party device libraries (e.g. navX)
    compile fileTree(dir: 'libs', include: '**/*.jar')
    native  fileTree(dir: 'libs', include: '**/*.so')
}
```