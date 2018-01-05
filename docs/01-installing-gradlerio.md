01 - Installing GradleRIO
====
This is the guide for installing GradleRIO for your project. If you're looking to update GradleRIO, see [here](02-updating-gradlerio.md).

## Step 1. Install Java
If you don't have Java installed on your system, you can download it from [here](https://java.com/en/download/). Any version 8 or higher will work just fine.

## Step 2. Download Quickstart
Download the Quickstart from [here](../Quickstart.zip). Extract the Quickstart and choose the directory for your language (either java or cpp), and copy it to your project directory.

Next, open up your `build.gradle` file and edit the team number to reflect your team.  

C++ Users: Your C++ source files (.cpp) will be in `src/cpp/`, and your header files (.h) will be in `src/include/`  
Java Users: Edit the `ROBOT_CLASS` variable to point to where your Robot class is. Your java source files will be in `src/main/java/`. You do not need to deploy java to your RoboRIO, GradleRIO will do it for you.

## Step 3. Run Gradle Tasks
All `./gradlew` tasks should be run in a command/terminal window. On Windows, it is recommended to use Powershell.

It is recommended to do this step at home, since some schools block access to certain websites, pertaining to an error.  

C++ Users: Run `./gradlew installToolchain`. Make sure you are connected to an internet connection.  
Java Users: Run `./gradlew build`. Make sure you are connected to an internet connection.

## Step 4. Use GradleRIO
That's it! You're now ready to use GradleRIO.  
Deploy code to your robot using `./gradlew deploy`  
Build but not deploy your code using `./gradlew build`