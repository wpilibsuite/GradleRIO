05 - Debugging
====

You can remotely debug your FRC program in both Java and C++.

## All Languages:
Edit your `build.gradle` so that it deploys debugging libs.
```gradle
deploy {
    artifacts {
        // Java
        artifact('frcJava', FRCJavaArtifact) {
            targets << "roborio"
            debug = true        // Add this line
        }

        // C++
        artifact('frcNative', FRCNativeArtifact) {
            targets << "roborio"
            component = 'frcUserProgram'
            debug = true        // Add this line
        }
    }
}
```

Run `./gradlew deploy`.

**NOTE: Enabling debugging will wait for your remote debugger to connect before starting the robot program. Be sure to remove the debug lines above before deploying for competition**

## Java:
Connect your IDE's debugger to your RoboRIO (address: roborio-TEAM-frc.local, port: 8348) and launch.  
Note: if the above address doesn't work, use the IP address that `./gradlew deploy` prints first.

## C++:
Install `gdb-multiarch` on your computer. On debian, this is `apt-get install gdb-multiarch`

When running `./gradlew deploy`, a message will be printed out with a `gdb` command. Copy this command into your terminal and run it, then use gdb like normal.