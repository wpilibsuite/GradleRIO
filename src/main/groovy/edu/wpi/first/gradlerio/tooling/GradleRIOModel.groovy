package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import org.gradle.tooling.model.Model

@CompileStatic
public interface GradleRIOModel extends Model {
  // This is the C++ Json configuration from
  // GradleVsCode. Models are not transmitted directly
  // because of tooling API limitations
  // In VsCode I need the strings anyway, so not an issue.
  String getCppConfigurations();
  boolean getHasJava();
  boolean getHasNative();
  // String of tools configurations
  // Same reason as above
  String getTools();
}
