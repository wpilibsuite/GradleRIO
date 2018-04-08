01 - Installing GradleRIO
====
This is the guide for installing GradleRIO for your project. If you're looking to update GradleRIO, see [here](02-updating-gradlerio.md).

## Step 1. Install Java
If you don't have Java installed on your system, you can download it from [here](https://java.com/en/download/). Any version 8 or higher will work just fine.

## Step 2. Download Quickstart
Download the Quickstart from [here](../Quickstart.zip). Extract the Quickstart files to a temporary location on your disk.

## Step 3a.  Option 1 - Create a new project
The quickstart files will give you the basis for a new robot project, which you can simply
use to begin creating a robot.  To do this, simply copy the entire directory for your language
(either java or cpp) to the location of your choice.  For Java, rename the team0000 directory to something
more to your liking.  For cpp, you will likely want to rename 'MyRobot.cpp' and 'MyHeader.h' to
something more to your liking.

## Step 3b.  Option 2 - Update an existing project
If you have an existing project, you will want to copy in most of the files
from the quickstart directory, but omit the sample Robot files.  For Java,
the samples files to omit are in the `src/main/java/frc/team0000` directory. For cpp,
you want to exclude the `src/cpp/Robot.cpp` and `src/include/MyHeader.h` files.

The easiest way to accomplish is to delete the sample files from your temporary
directory, and then just copy the remaining files from your temporary
directory over top of your existing code.

## Step 4.  Configure your project

Next, open up your `build.gradle` file and edit the team number to reflect your team.
Java users will also need to edit the `ROBOT_CLASS` variable in `build.gradle` to point to your Robot class.

**Note:** Make sure to use the directory layout suggested by the quickstart:
  - C++:  Source files (.cpp) in `src/cpp` and header files (.h) in `src/include`.
  - Java: Source files in `src/main/java/`.

## Step 5. Run Gradle Tasks
All `./gradlew` tasks should be run in a command/terminal window. On Windows, it is recommended to use Powershell.

It is recommended to do this step at home, since some schools block access to certain websites, pertaining to an error.  

C++ Users: Run `./gradlew installToolchain`. Make sure you are connected to an internet connection.  
Java Users: Run `./gradlew build`. Make sure you are connected to an internet connection.

## Step 6. Use GradleRIO
That's it! You're now ready to use GradleRIO.  
Deploy code to your robot using `./gradlew deploy`  
Build but not deploy your code using `./gradlew build`

**Note:** Java users do not need to deploy Java to their RoboRIO, as GradleRIO will do it for you.
