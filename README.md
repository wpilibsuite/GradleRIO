# GradleRIO
GradleRIO is a powerful Gradle Plugin that allows teams competing in the FIRST
robotics competition to produce and build their code.

![](img/tty.gif)

GradleRIO works with Java and C++ (and others!), on Windows, Mac and Linux. GradleRIO automatically fetches WPILib, CTRE Toolsuite (Talon SRX) and NavX libraries, and you can even add your own libraries!

For 2019+, GradleRIO is the official build system for the _FIRST_ Robotics Competition! The officially supported IDE is Visual Studio Code (VSCode), using the WPILib extension. 

Other IDEs like IntelliJ IDEA, Eclipse, Visual Studio, and CLion are also supported, unofficially. You may also use this tool exclusively from the command line, allowing use of any IDE or text editor (like Sublime Text, Atom or Vim).

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
_These IDEs are unofficially supported, so CSA support is not guaranteed. It is recommended to use VSCode, however these options are available if you wish._
- ```./gradlew idea``` will generate IDE files for IntelliJ IDEA (java)  
- ```./gradlew eclipse``` will generate IDE files for Eclipse (java)  
- ```./gradlew <component>VisualStudio``` will generate IDE files for the C/C++ component named `<component>` for Visual Studio (C++)
- ```./gradlew clion``` will generate IDE files for Clion (C++). Be warned that Clion support is hacky as Clion does not natively support Gradle.

## Upgrading
To upgrade your version of GradleRIO, you must first upgrade gradle. Near the bottom of your build.gradle, change the wrapper version to the following, and then run `./gradlew wrapper`:
```gradle
task wrapper(type: Wrapper) {
    gradleVersion = '4.7'
}
```

Next, replace the version in the plugin line (only change the GradleRIO line):
```gradle
plugins {
    // ... other plugins ...
    id "jaci.openrio.gradle.GradleRIO" version "2018.06.21"
}
```

## Implementation Details
**NOTE: This section is for advanced users. View the examples for your language to get started.**

### Full Spec
```gradle
deploy {
    targets {
        target('roborio', RoboRIO) {
            team = getTeamOrDefault(5333)
            // Other values can be edited through EmbeddedTools.
            // See https://github.com/JacisNonsense/EmbeddedTools#spec
        }
        // Other targets can be edited through EmbeddedTools.
        // See https://github.com/JacisNonsense/EmbeddedTools#spec
    }
    artifacts {
        // Setup a Java Artifact. Required for Java Users.
        artifact('myJava', FRCJavaArtifact) {
            targets << 'roborio'

            jvmArgs << '-Xmx=128m'      // Set more JVM Arguments. Optional.
            arguments << 'myCustomArgs' // The command-line arguments to launch your jar with. Optional.
            debug = getDebugOrDefault(false)                // Enable to enable java debugging on the RoboRIO. Default: false
            debugPort = 8348            // Set the debugging port. Default: 8348
            robotCommand = './myOtherProgram'       // Set the contents of robotCommand. Optional, usually created depending on above values.

            // Other values can be edited through EmbeddedTools.
            // See https://github.com/JacisNonsense/EmbeddedTools#spec
        }

        // Setup a C++ (Native) Artifact. Required for C++ (Native) Users
        artifact('myNative', FRCNativeArtifact) {
            targets << 'roborio'
            component = 'myFrcBinary'   // The name of the component you wish to build (required).

            arguments << 'myCustomArgs' // The command-line arguments to launch your jar with. Optional.
            debug = getDebugOrDefault(false)                // Enable to enable cpp debugging on the RoboRIO. Default: false
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
    ntcoreVersion = '...'
    opencvVersion = '...'
    cscoreVersion = '...'
    wpiutilVersion = '...'

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
}

// Java only. Setup your Jar File.
jar {
    // Compile a 'fat jar' (libraries included)
    from configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } 
    // Include your Manifest. Arguments are your Robot Main Class.
    manifest GradleRIOPlugin.javaManifest('test.myClass')
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
            }
            // Add the libraries you wish to use in your NATIVE project.
            // WPILib adds WPILibJ, NTCore, OpenCV, CSCore among others.
            // CTRE adds the CTRE Toolsuite (i.e. Talon SRX)
            // NavX adds the NavX IMU library.
            useLibrary(it, "wpilib", "navx", "ctre")
        }
    }
}