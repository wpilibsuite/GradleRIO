04 - Customizing GradleRIO
====

You can find most info for this in the [README](https://github.com/Open-RIO/GradleRIO#full-spec) as well as in [EmbeddedTools](https://github.com/JacisNonsense/EmbeddedTools#spec). There is a lot more to offer than what's just in this page, this is just what's commonly requested.

## Changing your RoboRIO IP Address
If, for some reason, you wish to use a custom IP address for your RoboRIO, you can add the following to the relevant section of your `build.gradle`:

```gradle
deploy {
    targets {
        target("roborio", jaci.openrio.gradle.frc.RoboRIO) {
            team = TEAM
            addresses << '10.MY.IP.2'   // Add this line
        }
    }
}
```

## Adding / Removing Vendor Libraries
GradleRIO supports NavX and CTRE Phoenix libraries out-of-the-box. By default, both are included, but you can remove them if you wish:

### Java:
```gradle
dependencies {
    // You can add/remove any of these as you wish
    compile wpilib()
    compile ctre()
    compile navx()
}
```

### C++:
```gradle
model {
    components {
        frcUserProgram(NativeExecutableSpec) {
            // ... ETC ... //

            // You can add/remove any of these as you wish
            lib library: "wpilib"
            lib library: "ctre"
            lib library: "navx"
        }
    }
}
```

## Changing Dependency Versions
GradleRIO automatically uses stable releases of all software, but if you wish to change them for any reason, you can do so in the `wpi {}` extension block in your `build.gradle`, e.g.
```gradle
wpi {
    wpilibVersion = "2018.1.1"

    navxVersion = "3.0.342"
}
```

You can see all these values in the [README](https://github.com/Open-RIO/GradleRIO#full-spec)

## Adding Custom Dependencies
If you want to add your own project dependencies, it is very easy to do so:

### Java:
Gradle Dependency Reference: https://docs.gradle.org/current/userguide/artifact_dependencies_tutorial.html
```gradle
dependencies {
    compile files('mylib.jar')
    compile 'some.maven:artifact:version'
}
```

### C++:
These are added through EmbeddedTools. Find more info here: https://github.com/JacisNonsense/EmbeddedTools#spec
```gradle
model {
    libraries {
        mylib(NativeLib) {
            targetPlatform 'roborio'

            // Either file or maven, but not both
            // Zip file must contain libraries built for the RoboRIO.
            file 'myfile.zip'
            maven 'some.maven:artifact:version@zip'

            // Edit these as appropriate for your .zip file
            headerDirs << 'include'
            libraryMatchers << '**/*.so'    // Deploy libs matcher
            sharedMatchers << '**/*.so'     // Shared library matcher
            staticMatchers << '**/*.a'      // Static library matcher
        }
    }

    components {
        frcUserProgram(NativeExecutableSpec) {
            // ... ETC ... //

            // You can add/remove any of these as you wish
            lib library: "mylib"
        }
    }
}
```
