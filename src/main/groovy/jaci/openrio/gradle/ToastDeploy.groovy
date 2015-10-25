package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;
import java.io.File;

public class ToastDeploy {

  static void init(Project project) {
    def deploy_task = project.task('toastDeploy') << {
      def toast_resource = getToastResource(project)
      def nashorn = new File("build/caches/GradleRIO/nashorn.jar")

      GradleRIO.exportCaches()
      GradleRIO.tryOnAll(project) {
        scp(project, it, toast_resource, nashorn)
      }
    }
    deploy_task.setDescription "Deploy Toast to the RoboRIO"
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

  public static File getToastResource(Project project) {
    def comp = project.getConfigurations().compile
    def toast_dep = comp.getDependencies().find {
      it.getName() == "Toast" && it.getGroup() == "jaci.openrio.toast"
    }
    def toast_artifact = comp.files(toast_dep)[0]
    return toast_artifact
  }
}
