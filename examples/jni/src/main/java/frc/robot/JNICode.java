/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import java.io.IOException;

import edu.wpi.first.wpiutil.RuntimeLoader;

public class JNICode {
  static RuntimeLoader<JNICode> loader = null;

  static {
    try {
      // Change this name to match the library name of the jni library in build.gradle
      // For the last parameter, make it match this class
      loader = new RuntimeLoader<>("JNILibrary", RuntimeLoader.getDefaultExtractionRoot(), JNICode.class);
      loader.loadLibrary();
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public static native void jniFunction();
}
