package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import org.gradle.tooling.model.Model
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolInfo

@CompileStatic
public interface GradleRIOModel extends Model {
  boolean getHasJava();
  boolean getHasNative();
  String getRoboRIOCompiler();
  List<WPIToolInfo> getTools();
}
