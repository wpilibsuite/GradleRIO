package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPITool

@CompileStatic
class GradleRIOToolingExtension {
  List<WPITool> tools = []
}
