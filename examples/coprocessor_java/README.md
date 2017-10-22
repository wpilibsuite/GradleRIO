Coprocessor
---

This example involves the usage of a Coprocessor, in this case, the Raspberry Pi.

We'll be using Java in this example, but you can also use C++ given a proper toolchain. See [examples/coprocessor_cpp](../coprocessor_cpp) for an example. You can also use a combination (e.g. Java on the RoboRIO, C++ on the Coprocessor) if you wish.

## Projects
This example makes use of multi-project builds, an extremely powerful feature of gradle. In this example, there are two projects:
- [coprocessor](coprocessor) contains the code for our coprocessor (the Raspberry Pi)
- [robot](robot) contains the code for the robot (the RoboRIO)

The root project links these projects together through the use of [settings.gradle](settings.gradle). Each project has its own build.gradle, as does the root project.

NOTE: The coprocessor project uses EmbeddedTools, the backing library behind GradleRIO. It features everything that GradleRIO does, without the frc-specific
stuff, which is why we use it on the coprocessor instead of GradleRIO.