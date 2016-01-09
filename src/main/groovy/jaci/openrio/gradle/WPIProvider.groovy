package jaci.openrio.gradle;

import org.gradle.api.*;
import java.util.jar.*;
import java.io.*;

import org.gradle.logging.StyledTextOutput;
import org.gradle.logging.StyledTextOutputFactory;
import static org.gradle.logging.StyledTextOutput.Style;

public class WPIProvider {

  public static def flavour = "GRADLERIO"

  public static void init(Project project) {
    readManifest()
    if (flavour == "GRADLERIO") {
      addWPILibraries(project)
    } else if (flavour == "TOAST") {
      Toast.init(project)
      ToastDeploy.init(project)
    }
  }

  public static void addWPILibraries(Project project) {
    project.repositories.maven {
      it.name = "WPI"
      it.url = "http://first.wpi.edu/FRC/roborio/maven/release"
    }
    
    project.getConfigurations().maybeCreate("wpi_source")
    
    def deps = project.dependencies
    
    deps.add("compile", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:+:desktop')
    deps.add("compile", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:+:arm')
    
    deps.add("compile", 'edu.wpi.first.wpilibj:wpilibJavaFinal:+')
    
    deps.add("wpi_source", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:+:sources')
    deps.add("wpi_source", 'edu.wpi.first.wpilibj:wpilibJavaFinal:+:sources')
  }

  public static boolean isToast() {
    return flavour == "TOAST"
  }

  public static void readManifest() {
    try {
      InputStream st
      def en = WPIProvider.class.getClassLoader().getResources("META-INF/MANIFEST.MF")
      while (en.hasMoreElements()) {
        URL url = en.nextElement();
        if (url.getPath().contains("GradleRIO")) {
          st = url.openStream();
        }
      }
      Manifest mf = new Manifest(st)
      flavour = mf.getMainAttributes().getValue("GradleRIO-Flavour")
    } catch (Exception e){
      e.printStackTrace()
    }
  }

}
