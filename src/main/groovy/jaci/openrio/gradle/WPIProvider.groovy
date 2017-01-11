package jaci.openrio.gradle;

import org.gradle.api.Project
import java.util.jar.Manifest

public class WPIProvider {

  public static def flavour = "GRADLERIO"

  public static void init(Project project) {
    readManifest()
    if (flavour == "GRADLERIO") {
//      addWPILibraries(project)
    } else if (flavour == "TOAST") {
      Toast.init(project)
    }
  }

  public static void addWPILibraries(Project project) {
    project.repositories.maven {
      it.name = "WPI"
      it.url = "http://first.wpi.edu/FRC/roborio/maven/" + project.gradlerio.wpi_branch
    }
    
    project.getConfigurations().maybeCreate("wpi_source")
    
    def deps = project.dependencies
    def wpi_version = project.gradlerio.wpilib_version
    def nt_version = project.gradlerio.ntcore_version
    
    deps.add("compile", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:' + nt_version + ':desktop')
    deps.add("compile", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:' + nt_version + ':arm')
    
    deps.add("compile", 'edu.wpi.first.wpilibj:wpilibJavaFinal:' + wpi_version)
    
    deps.add("wpi_source", 'edu.wpi.first.wpilib.networktables.java:NetworkTables:' + nt_version + ':sources')
    deps.add("wpi_source", 'edu.wpi.first.wpilibj:wpilibJavaFinal:' + wpi_version + ':sources')
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
