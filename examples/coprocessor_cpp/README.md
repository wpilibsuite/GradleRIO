Coprocessor
---

This example involves the usage of a Coprocessor, in this case, the Raspberry Pi.

We'll be using C++ in this example, but you can also use Java. See [examples/coprocessor_java](../coprocessor_java) for an example. You can also
use a combination (e.g. Java on the RoboRIO, C++ on the Coprocessor) if you wish.

## Projects
This example makes use of multi-project builds, an extremely powerful feature of gradle. In this example, there are two projects:
- [coprocessor](coprocessor) contains the code for our coprocessor (the Raspberry Pi)
- [robot](robot) contains the code for the robot (the RoboRIO)

The root project links these projects together through the use of [settings.gradle](settings.gradle). Each project has its own build.gradle, as does the root project.

NOTE: The coprocessor project uses EmbeddedTools, the backing library behind GradleRIO. It features everything that GradleRIO does, without the frc-specific
stuff, which is why we use it on the coprocessor instead of GradleRIO.

## Toolchains
Note that you will need a toolchain in order to cross-compile for your coprocessor. You can usually find these specific to your desired target, but in this case we'll be using the raspberry pi toolchains found at https://github.com/raspberrypi/tools

You will also need to run this on a Linux system, as the Raspberry Pi toolchain uses symbolic links for its libraries.