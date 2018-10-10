package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import edu.wpi.first.vscode.tooling.models.ToolChains
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolInfo
import java.io.Serializable

@CompileStatic
class DefaultGradleRIOModel implements GradleRIOModel, Serializable {
  private final Set<ToolChains> cppConfiguration;
  private final boolean hasJava;
  private final boolean hasNative;
  private final String roboRIOCompiler;
  private final List<WPIToolInfo> tools;

  DefaultGradleRIOModel(Set<ToolChains> cppConfiguration, boolean hasNative, boolean hasJava,
                            String roboRIOCompiler, List<WPIToolInfo> tools) {
    this.cppConfiguration = cppConfiguration;
    this.hasJava = hasJava;
    this.hasNative = hasNative;
    this.roboRIOCompiler = roboRIOCompiler;
    this.tools = tools;
  }

  @Override
  Set<ToolChains> getCppConfigurations() {
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
  String getRoboRIOCompiler() {

  }

  @Override
  List<WPIToolInfo> getTools() {
    return tools;
  }
}
