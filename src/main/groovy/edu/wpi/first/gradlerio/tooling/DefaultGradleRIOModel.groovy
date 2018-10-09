package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import java.io.Serializable

@CompileStatic
class DefaultGradleRIOModel implements GradleRIOModel, Serializable {
  private static final long serialVersionUID = 6218239197021120278L;

  private final String cppConfiguration;
  private final boolean hasJava;
  private final boolean hasNative;
  private final String tools;

  DefaultGradleRIOModel(String cppConfiguration, boolean hasJava, boolean hasNative,
                               String tools) {
    this.cppConfiguration = cppConfiguration;
    this.hasJava = hasJava;
    this.hasNative = hasNative;
    this.tools = tools;
  }

  @Override
  String getCppConfigurations() {
    return cppConfiguration;
  }

  @Override
  boolean getHasJava() {
    return hasJava;
  }

  @Override
  boolean getHasNative() {
    return hasNative;
  }

  @Override
  String getTools() {
    return tools;
  }
}
