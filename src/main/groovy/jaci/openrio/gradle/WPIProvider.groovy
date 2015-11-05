package jaci.openrio.gradle;

import org.gradle.api.*;
import java.util.jar.*;
import java.io.*;

public class WPIProvider {

  public static def flavour = "GRADLERIO"

  public static void init(Project project, String apiDest) {
    readManifest()
    if (flavour == "GRADLERIO") {
      addWPILibraries(project, apiDest)
    } else if (flavour == "TOAST") {
      Toast.init(project)
      ToastDeploy.init(project)
    }
  }

  public static boolean isToast() {
    return flavour == "TOAST"
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

  public static void update(Project project) {
    String extractedDest = System.getProperty("user.home") + "/wpilib/java/extracted/current/"
    String urlBase = "http://first.wpi.edu/FRC/roborio/release/eclipse/"
    String wpiVersion = "java_0.1.0.201501221609"
    println "Checking WPILib Version..."

    String wpiInstalledVersion = ""
    try {
      def versionXML=new XmlSlurper().parse(GradleRIO.pluginDest+"content/content.xml")
      def vNode = versionXML.depthFirst().find{it.@id == 'edu.wpi.first.wpilib.plugins.java'}
      wpiInstalledVersion = vNode.@version
      println "Currently Installed WPILib Version: ${wpiInstalledVersion}"
    } catch (Exception e) {  }

    try {
      GradleRIO.download(GradleRIO.pluginDest, urlBase+"content.jar", "content.jar")
      project.ant.unzip(src: GradleRIO.pluginDest+"content.jar",
        dest: GradleRIO.pluginDest+"content",
        overwrite:"true")

      def xml=new XmlSlurper().parse(GradleRIO.pluginDest+"content/content.xml")
      def node = xml.depthFirst().find{it.@id == 'edu.wpi.first.wpilib.plugins.java'}
      String wpiVersionLatest = node.@version
      println "WPILib Latest Version: ${wpiVersionLatest}"

      if (wpiInstalledVersion != wpiVersionLatest) {
        println "WPILib Version Mismatch... Updating..."
        wpiVersion = "java_${wpiVersionLatest}"
      } else {
        println "WPILib Version Match. Skipping Update..."
        return;
      }

      println "Deleting WPILib Caches..."
      project.ant.delete(dir: extractedDest)
    } catch (Exception e) {
      println "Could not check WPI Version..."
      println e
      return
    }

    String from = urlBase + "plugins/edu.wpi.first.wpilib.plugins.${wpiVersion}.jar"
    println "Downloading WPILib..."
    GradleRIO.download(GradleRIO.pluginDest, from, "plugin.jar")
    println "Extracting WPILib..."

    project.ant.unzip(src:GradleRIO.pluginDest+"plugin.jar",
      dest:extractedDest,
      overwrite:"false")
    println "WPILib Extracted..."
    println "Extracting API Resources..."
    project.ant.unzip(src:extractedDest+"resources/java.zip",
      dest:GradleRIO.apiDest,
      overwrite:"false")
    println "API Resources extracted..."
  }

}
