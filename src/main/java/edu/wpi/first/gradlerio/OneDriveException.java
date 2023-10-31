package edu.wpi.first.gradlerio;

import org.gradle.api.Project;

public class OneDriveException extends java.lang.RuntimeException {
  public OneDriveException(Project project) {
    super(String.format("Error cannot create project inside OneDrive. Project Directory = %S", project.getRootDir().toString()));
    System.out.println(String.format("Error cannot create project inside OneDrive. Project Directory = %S", project.getRootDir().toString()));
    _project = project;
  }

  public void printError() {
    System.out.println(String.format("Error cannot create project inside OneDrive. Project Directory = %S", _project.getRootDir().toString()));
  }

  private Project _project;
}
