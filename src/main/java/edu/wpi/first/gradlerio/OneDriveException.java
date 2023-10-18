package edu.wpi.first.gradlerio;

import java.lang.Exception;

public class OneDriveException extends java.lang.RuntimeException {
  public OneDriveException() {
    super(String.format("Error cannot create project inside OneDrive. Project Directory = %S", System.getProperty("user.dir")));
    PrintMessage();
  }

  public void PrintMessage() {
    System.out.println(String.format("Error cannot create project inside OneDrive. Project Directory = %S", System.getProperty("user.dir")));
  }

}
