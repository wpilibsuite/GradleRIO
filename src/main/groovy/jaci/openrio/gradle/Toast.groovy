package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;

public class Toast {

  static void init(Project project) {
    project.getConfigurations().maybeCreate('verify')

    def verifyTask = project.task('verify') << {
      File toast_dir = new File('toast_verification')
      if (toast_dir.exists()) {
        project.ant.delete(dir: toast_dir.getAbsolutePath())
      }

      toast_dir.mkdirs()
      File modules_dir = new File(toast_dir, "toast/modules")
      modules_dir.mkdirs()

      File toast = new File(toast_dir, "Toast.jar")
      println "Copying Assets..."
      project.getConfigurations().compile.resolve().each {
        File file = it
        String absPath = file.getAbsolutePath()
        if (absPath.contains("jaci.openrio.toast") && file.getName().contains("Toast")) {
          project.ant.copy(tofile: toast.getAbsolutePath(), file: absPath)
        }
      }
      println "Toast Successfully Copied..."
      println "Copying Verification Modules..."
      project.getConfigurations().verify.resolve().each {
        File file = it
        File toFile = new File(modules_dir, file.getName())
        project.ant.copy(tofile: toFile.getAbsolutePath(), file: file.getAbsolutePath())
      }
      String archive = project.jar.archivePath
      File archiveTo = new File(modules_dir, new File(archive).getName())
      project.ant.copy(tofile: archiveTo.getAbsolutePath(), file: archive)
      println "Verification Assets Copied..."
      println "Beginning Verification..."
      project.javaexec {
        workingDir = toast_dir.getAbsolutePath()
        args = ["Toast.jar", "-verify"]
        main = '-jar'
      }
      println "Verification Complete..."
    }
  }

}
