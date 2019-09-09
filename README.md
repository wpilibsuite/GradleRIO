# GradleRIO
GradleRIO is a powerful Gradle Plugin that allows teams competing in the FIRST
robotics competition to produce and build their code.

![](img/tty.gif)

GradleRIO works with Java and C++ (and others!), on Windows, Mac and Linux. GradleRIO automatically fetches WPILib, Tools, and Vendor Libraries.

For 2019+, GradleRIO is the official build system for the _FIRST_ Robotics Competition! The officially supported IDE is Visual Studio Code (VSCode), using the [WPILib Extension](https://github.com/wpilibsuite/vscode-wpilib).

Screensteps is the best place for documentation: https://wpilib.screenstepslive.com/s/currentCS/m/79833

Other IDEs like IntelliJ IDEA, Eclipse, Visual Studio, and CLion are also supported, unofficially. You may also use this tool exclusively from the command line, allowing use of any IDE or text editor (like Sublime Text, Atom or Vim).

## Getting Started - Creating a new project
### With VSCode (recommended)
For getting started with VSCode, please see the screensteps documentation:
https://wpilib.screenstepslive.com/s/currentCS/m/79833

### Without VSCode
Follow the installation instructions on screensteps: https://wpilib.screenstepslive.com/s/currentCS/m/79833
_Note that the offline installer isn't required, but will save you a ton of time and is highly recommended. You can deselect the option of VSCode if you wish._

**WPILibUtility Standalone Project Builder**
WPILib provides a standalone project builder that provides the same interface as VSCode, without having to use VSCode.

If you've used the installer, find and run `wpilibutility` in `C:\Users\Public\wpilib\2020\utility` (windows), or `~/wpilib/2020/utility`(mac/linux). Note that mac users will have to extract the .tar.gz file, then run.
Alternatively, download it from the VSCode-WPILib releases, extract it, and run it: https://github.com/wpilibsuite/vscode-wpilib/releases

Use the WPILib Utility whenever you want to create a new project.

**GradleRIO Example Project**
Go to the latest release on GitHub: https://github.com/wpilibsuite/GradleRIO/releases.
Download the .zip file corresponding to your language and extract it.

## Adding Vendor Libraries
### With VSCode
Open the command palette with CTRL + SHIFT + P, or by clicking the WPILib icon.
Open `WPILib: Manage Vendor Libraries`, `Install new libraries (online)`, and paste the vendor-provided JSON url.

### Without VSCode
Create a folder `vendordeps` in your project directory if it doesn't already exist.
Download the JSON file from the vendor-provided URL, and save it to the `vendordeps` folder.

## Commands
Windows Users: It is recommended to use Powershell instead of CMD. You can switch to powershell with `powershell`

### General
- `./gradlew build` will build your robot code (and run unit tests if present).
- `./gradlew deploy` will build and deploy your code.
- `./gradlew riolog` will display the RoboRIO console output on your computer (run with `-Pfakeds` if you don't have a driverstation connected).

- `./gradlew installRoboRioToolchain` will install the C++ Toolchains for your system (required for C++).

### Tools
- `./gradlew ShuffleBoard` will launch Shuffleboard, the 2018 replacement for SmartDashboard.
- `./gradlew SmartDashboard` will launch Smart Dashboard (note: SmartDashboard is legacy software, use ShuffleBoard instead!).
- `./gradlew RobotBuilder` will launch Robot Builder, a tool for generating robot projects and source files.
- `./gradlew OutlineViewer` will launch Outline Viewer, for viewing NetworkTables.
- `./gradlew PathWeaver` will launch PathWeaver, a tool for generating motion profiles with Pathfinder.

**At Competition? Connected to the Robot?** Run with the `--offline` flag. e.g. `./gradlew deploy --offline`

## IDE Support
### VSCode:
VSCode is fully supported by GradleRIO for FRC. To use it, use the WPILib VSCode extension. See screensteps for instructions.

### IntelliJ IDEA:
_IntelliJ IDEA support is unofficial in the FRC sense, but is well supported by the Gradle team. CSA Support isn't guaranteed, so make sure you're prepared to fix any issues yourself if you're at an event._

To start with, you must apply the `idea` plugin to build.gradle. In your `build.gradle`, put the following code in the `plugins {}` block.
```gradle
plugins {
    id 'idea'
}
```

You can generate your project with the following command:
- `./gradlew idea` will generate IDE files for IntelliJ IDEA (java).

Import your project with `File - Open Project` in IntelliJ IDEA.

Please see the gradle guide on the idea plugin for help: https://docs.gradle.org/current/userguide/idea_plugin.html

### Eclipse
_Eclipse support is unofficial in the FRC sense, but is well supported by the Gradle team. CSA Support isn't guaranteed, so make sure you're prepared to fix any issues yourself if you're at an event. **Eclipse is only supported for JAVA (not C++)**_

To start with, you must apply the `eclipse` plugin to build.gradle. In your `build.gradle`, put the following code in the `plugins {}` block.
```gradle
plugins {
    id 'eclipse'
}
```

You can generate your project with the following command:
- `./gradlew eclipse` will generate IDE files for Eclipse (java).

Import your project with `File - Import... Existing Projects into Workspace` in Eclipse.

Please see the gradle guide on the eclipse plugin for help: https://docs.gradle.org/current/userguide/eclipse_plugin.html

### Visual Studio 2017 Community / Full (not VSCode)
_VS2017 support is unofficial in the FRC sense, but is well supported by the Gradle team. CSA Support isn't guaranteed, so make sure you're prepared to fix any issues yourself if you're at an event._

To start with, you must apply the `visual-studio` plugin to build.gradle. In your `build.gradle`, put the following code in the `plugins {}` block.
```gradle
plugins {
    id 'visual-studio'
}
```

Finally, you can generate and open your solution with the following command:
- `./gradlew openVisualStudio` will generate IDE files for VS2017 (C++) and open Visual Studio.

Please see the gradle guide on building native software for help: https://docs.gradle.org/current/userguide/native_software.html#native_binaries:visual_studio

## Upgrading
To upgrade your version of GradleRIO, you must first upgrade gradle. Near the bottom of your build.gradle, change the wrapper version to the following, and then run `./gradlew wrapper`:
```gradle
task wrapper(type: Wrapper) {
    gradleVersion = '5.0'
}
```

Next, replace the version in the plugin line (only change the GradleRIO line):
```gradle
plugins {
    // ... other plugins ...
    id "edu.wpi.first.GradleRIO" version "REPLACE ME WITH THE LATEST VERSION"
}
```

The latest version can be obtained from here: https://plugins.gradle.org/plugin/edu.wpi.first.GradleRIO
