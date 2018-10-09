package edu.wpi.first.gradlerio.tooling

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.PluginContainer
import org.gradle.tooling.provider.model.ToolingModelBuilder

import edu.wpi.first.vscode.GradleVsCode
import edu.wpi.first.vscode.tooling.ToolChainGenerator
import edu.wpi.first.vscode.tooling.models.ToolChains

@CompileStatic
class GradleRIOToolingModelBuilder implements ToolingModelBuilder {

  @Override
  Object buildAll(String modelName, Project project) {
    Set<ToolChains> toolChains = null;
    boolean hasNative = false;
    def plugins = project.plugins;
    def toolExtension = project.extensions.getByType(GradleRIOToolingExtension)

    // If VS Code plugin is applied, generate toolchains
    // Any native project applies it, so also implies native
    if (plugins.hasPlugin(GradleVsCode)) {
      try {
        toolChains = ToolChainGenerator.generateToolChains(project);
      } catch (Exception ex) {
        // Catch any exception generating toolchains
      }
      hasNative = true;
    }

    def hasJava = plugins.hasPlugin(JavaPlugin);

    def tools = toolExtension.tools.collect { it.WPIToolInfo }

    return new DefaultGradleRIOModel(toolChains, hasNative, hasJava, tools);
  }

  @Override
  boolean canBuild(String modelName) {
    return modelName == GradleRIOModel.class.name;
  }
}
