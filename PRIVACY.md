Privacy Policy
===

GradleRIO collects some usage data during the build to help with future development. This data is anonymous, and is not available to the public. You may also opt out of this by disabling telemetry by adding the following to your build.gradle. 
```gradle
telemetry {
    reportTelemetry = false
}
```
GradleRIO does not report telemetry when disabled as above, nor will it report if Gradle is run in an offline environment (e.g. with the `--offline` flag, or with no internet connection).


## What data?
You can see the exact data GradleRIO reports by running `./gradlew telemetry`.   
GradleRIO reports the following data:  
- `UUID`: A unique, randomly generated User ID.
- `OS`: Some information about the operating system (type, version, CPU architecture).  
- `gradle`: Gradle version
- `plugins`: List of applied Gradle plugins (this helps us implement support for new languages like Kotlin, and new IDEs)  
- `classpath`: List of Gradle plugin dependencies  
- `deploy`: Some information on deployed targets
    - `targets`: Types of targets (e.g. RoboRIO). If a RoboRIO, also reports the team number (this helps avoid duplicate entries for multiple users on the same team)
    - `artifacts`: FRC-specific artifact types (this determines if GradleRIO uses the Java or C++ deploy logic)
- `dependencies`: List of project dependencies and their versions. This helps determine what versions of libraries are being used (e.g. wpilib/ctre/navx version), allowing extended support for other libraries.
- `wpi`: The configuration of the `wpi` block, including versions of wpilib, ntcore, opencv, shuffleboard, toolchain, etc.

## Distribution
Your user/team-specific usage data will not be made publically available. Period.  

The only public uses for the data as a whole are:  
- Demographic report (how many teams use GradleRIO, how many teams per country/state/region)
- OS Demographic report (what percentage of users use windows, mac, linux)
- Language Demographic report (how many teams use java/cpp/kotlin/others)
- Version Demographic report (how many teams use the kickoff release, how many use later/earlier releases)

Any public releases of the data will **NOT** show your team number, nor your unique user ID. Public defines any distribution at all. 

Teams may ask what data we hold for them (build counts, unique users, etc) and we will be happy to forward them that information in the same forum from which it was asked, unedited. (Ammendment 1)

## How can I verify this?
The code that runs the telemetry is completely Open-Source. The Gradle side can be seen [here](https://github.com/Open-RIO/GradleRIO/tree/master/src/main/groovy/jaci/openrio/gradle/telemetry), and the server side can be seen [here](https://github.com/JacisNonsense/imjac.in_ta/blob/master/modules/openrio/openrio_library.rb).
