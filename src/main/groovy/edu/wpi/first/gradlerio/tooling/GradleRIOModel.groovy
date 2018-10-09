package edu.wpi.first.gradlerio.tooling

import groovy.transform.CompileStatic
import org.gradle.tooling.model.Model
import edu.wpi.first.vscode.tooling.models.ToolChains
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolInfo

@CompileStatic
public interface GradleRIOModel extends Model {
  Set<ToolChains> getCppConfigurations();
  boolean getHasJava();
  boolean getHasNative();
  List<WPIToolInfo> getTools();
}
