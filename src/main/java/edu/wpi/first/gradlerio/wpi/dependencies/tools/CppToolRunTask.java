package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import edu.wpi.first.gradlerio.SingletonTask;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;

public class CppToolRunTask extends DefaultTask implements SingletonTask {
  private final Property<String> toolName;
  private final DirectoryProperty toolsFolder;

  @Internal
  public Property<String> getToolName() {
    return toolName;
  }

  @Internal
  public DirectoryProperty getToolsFolder() {
    return toolsFolder;
  }

  @Inject
  public CppToolRunTask(ObjectFactory objects) {
    setGroup("GradleRIO");

    this.toolName = objects.property(String.class);
    toolsFolder = objects.directoryProperty();
  }

  @TaskAction
  public void runTool() {
    boolean isWindows = OperatingSystem.current().isWindows();
    if (isWindows) {
      runToolWindows();
    } else {
      runToolUnix();
    }
  }

  private String getArgumentPath(String toolNameLower) {
    return new File(getProject().getProjectDir(), "." + toolNameLower).getAbsolutePath();
  }

  private void runToolWindows() {
    Directory toolsFolder = this.toolsFolder.get();
    String toolName = this.toolName.get();
    File outputFile = toolsFolder.file(toolName + ".vbs").getAsFile();
    ProcessBuilder builder =
        new ProcessBuilder(
            "wscript.exe",
            outputFile.getAbsolutePath(),
            "silent",
            getArgumentPath(toolName.toLowerCase()));
    try {
      Process proc = builder.start();
      int result = proc.waitFor();
      if (result != 0) {
        String stdOut = IOGroovyMethods.getText(proc.getInputStream());
        String stdErr = IOGroovyMethods.getText(proc.getErrorStream());
        throw new ToolRunException(stdOut, stdErr);
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void runToolUnix() {
    Directory toolsFolder = this.toolsFolder.get();
    String toolName = this.toolName.get();
    File outputFile = toolsFolder.file(toolName + ".py").getAsFile();
    getProject()
        .exec(
            spec -> {
              spec.setExecutable(outputFile.getAbsolutePath());
              spec.args(getArgumentPath(toolName.toLowerCase()));
            });
  }

  @Override
  @Internal
  public Provider<String> getSingletonName() {
    return toolName;
  }
}
