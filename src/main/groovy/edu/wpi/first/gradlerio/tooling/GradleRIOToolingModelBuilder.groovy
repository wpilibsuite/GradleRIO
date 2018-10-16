package edu.wpi.first.gradlerio.tooling

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.PluginContainer
import org.gradle.tooling.provider.model.ToolingModelBuilder

import jaci.gradle.toolchains.ToolchainsPlugin
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin

@CompileStatic
class GradleRIOToolingModelBuilder implements ToolingModelBuilder {

  @Override
  Object buildAll(String modelName, Project project) {
    boolean hasNative = false;
    String roboRIOCompiler = null;
    def plugins = project.plugins;
    def toolExtension = project.extensions.getByType(GradleRIOToolingExtension)

    // Having Toolchains means native projects were enabled
    if (plugins.hasPlugin(ToolchainsPlugin)) {
      def discoverer = plugins.getPlugin(WPIToolchainPlugin).maybeDiscoverRoborioToolchain()
      if (discoverer != null && discoverer.valid()) {
        def gccFile = discoverer.gccFile()
        if (gccFile.isPresent()) {
          roboRIOCompiler = gccFile.get().toString();
        }
      }
      hasNative = true;
    }

    def hasJava = plugins.hasPlugin(JavaBasePlugin);

    def tools = toolExtension.tools.collect { it.WPIToolInfo }

    return new DefaultGradleRIOModel(hasNative, hasJava, roboRIOCompiler, tools);
  }

  @Override
  boolean canBuild(String modelName) {
    return modelName == GradleRIOModel.class.name;
  }
}
