package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;

public class Toast {

  static void init(Project project) {
    ToastIDE.init(project)
    ToastConsole.init(project)
    project.getConfigurations().maybeCreate('verify')
    project.getConfigurations().maybeCreate('simulation')

    def verifyTask = project.task('verify') << {
      File toast_dir = new File('run/gradle/verification')
      if (toast_dir.exists()) {
        project.ant.delete(dir: toast_dir.getAbsolutePath())
      }

      toast_dir.mkdirs()
      File modules_dir = new File(toast_dir, "toast/modules")
      File libraries_dir = new File(toast_dir, "toast/libs")
      modules_dir.mkdirs()
      libraries_dir.mkdirs()

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
      
      project.getConfigurations().toastLibrary.resolve().each {
        File file = it
        File toFile = new File(libraries_dir, file.getName())
        project.ant.copy(tofile: toFile.getAbsolutePath(), file: file.getAbsolutePath())
      }
      
      project.getConfigurations().toastModule.resolve().each {
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
    verifyTask.setDescription "Run a Toast Code Verification on the module"

    def simTask = project.task('simulation') << {
      File rundir = new File('run/gradle/simulation')
      if (rundir.exists()) {
        project.ant.delete(dir: rundir.getAbsolutePath())
      }
      rundir.mkdirs()
      File modules_dir = new File(rundir, "toast/modules")
      File libraries_dir = new File(rundir, "toast/libs")
      modules_dir.mkdirs()
      libraries_dir.mkdirs()

      File toast_file = ToastDeploy.getToastResource(project)
      println "Copying Simulation Modules..."
      project.getConfigurations().simulation.resolve().each {
        File file = it
        File toFile = new File(modules_dir, file.getName())
        project.ant.copy(tofile: toFile.getAbsolutePath(), file: file.getAbsolutePath())
      }
      
      project.getConfigurations().toastLibrary.resolve().each {
        File file = it
        File toFile = new File(libraries_dir, file.getName())
        project.ant.copy(tofile: toFile.getAbsolutePath(), file: file.getAbsolutePath())
      }
      
      project.getConfigurations().toastModule.resolve().each {
        File file = it
        File toFile = new File(modules_dir, file.getName())
        project.ant.copy(tofile: toFile.getAbsolutePath(), file: file.getAbsolutePath())
      }
      
      String archive = project.jar.archivePath
      File archiveTo = new File(modules_dir, new File(archive).getName())
      project.ant.copy(tofile: archiveTo.getAbsolutePath(), file: archive)
      println "Simulation Assets Copied..."
      println "Beginning Simulation..."
      project.javaexec {
        workingDir = rundir.getAbsolutePath()
        args = [toast_file.getAbsolutePath(), "-sim"]
        main = '-jar'
      }
    }
    simTask.setDescription "Run a Toast Simulation on the module"
    
    def fetchTask = project.task('fetch') << {
      GradleRIO.tryOnAll(project) {
        def date = new Date()
        def formattedDate = date.format('yyyyMMddHHmmss')
        File remote = new File('remote/')
        remote.mkdirs()
        
        project.ant.scp(file:"lvuser@${host}:toast/*",
            todir:project.file('remote/${host}-${formattedDate}'),
            password:"",
            port:22,
            trust:true)
      }
    }
    fetchTask.setDescription "Copy all resources under '/home/lvuser/toast/' on the RoboRIO to the 'remote' directory in the project root"
  }

}
