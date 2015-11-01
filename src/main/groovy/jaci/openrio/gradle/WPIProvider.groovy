package jaci.openrio.gradle;

import org.gradle.api.*;
import java.util.jar.*;
import java.io.*;

public class WPIProvider {

  public static def flavour = "GRADLERIO"

  public static void doDeps(Project project, String apiDest) {
    readManifest()
    if (flavour == "GRADLERIO") {
      addWPILibraries(project, apiDest)
    } else if (flavour == "TOAST") {
      Toast.init(project)
      ToastDeploy.init(project)
    }
  }

  public static void addWPILibraries(Project project, String apidest) {
    project.repositories.flatDir() {
      dirs "${apidest}/lib"
    }
    project.dependencies.add('compile', ":WPILib")
    project.dependencies.add('compile', ":NetworkTables")
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
