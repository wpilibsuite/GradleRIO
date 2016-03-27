package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;
import java.io.File;

public class ToastDeploy {

  static void init(Project project) {
    project.getConfigurations().maybeCreate('toastLibrary')
    project.getConfigurations().maybeCreate('toastModule')

    prepareManualLibraries(project);

    def deploy_task = project.task('toastDeploy') << {
      def toast_resource = getToastResource(project)
      def nashorn = new File("build/caches/GradleRIO/nashorn.jar")

      GradleRIO.exportCaches()
      GradleRIO.tryOnAll(project) {
        scp(project, it, toast_resource, nashorn)
      }
    }
    deploy_task.setDescription "Deploy Toast to the RoboRIO"
    deploy_task.finalizedBy 'rioModeRun'
  }

  static prepareManualLibraries(Project project) {
      def deps = project.dependencies
      deps.add("compile", fileTree(dir: 'libs', include: '*.jar'))
      deps.add("compile", fileTree(dir: 'modules', include: '*.jar'))
      deps.add("toastLibrary", fileTree(dir: 'libs', include: '*.jar'))
      deps.add("toastModule", fileTree(dir: 'modules', include: '*.jar'))
  }

  static scp(Project project, String host, File toast, File nashorn) {
    project.ant.scp(file: toast.getAbsolutePath(),
      todir:"lvuser@${host}:/home/lvuser/FRCUserProgram.jar",
      password:"",
      port:22,
      trust:true)
    project.ant.scp(file: nashorn.getAbsolutePath(),
      todir:"admin@${host}:/usr/local/frc/JRE/lib/ext/nashorn.jar",
      password:"",
      port:22,
      trust:true)
  }

  public static void mapToastDeps(Project project) {
    def lib_config = project.getConfigurations().toastLibrary
    def libraries = lib_config.dependencies.collect {
      [file: lib_config.files(it)[0], name: it.getName()]
    }

    def mod_config = project.getConfigurations().toastModule
    def modules = mod_config.dependencies.collect {
      [file: mod_config.files(it)[0], name: it.getName()]
    }

    libraries.each {
      project.gradlerio.deployers += [ to: "/home/lvuser/toast/libs/${it.name}.jar", from: it.file ]
    }

    modules.each {
      project.gradlerio.deployers += [ to: "/home/lvuser/toast/modules/${it.name}.jar", from: it.file ]
    }
  }

  public static File getToastResource(Project project) {
    def comp = project.getConfigurations().compile
    def toast_dep = comp.getDependencies().find {
      it.getName() == "Toast" && it.getGroup() == "jaci.openrio.toast"
    }
    def toast_artifact = comp.files(toast_dep)[0]
    return toast_artifact
  }
}
