package jaci.openrio.gradle;

import org.gradle.api.*;

class GradleRIO implements Plugin<Project> {

  def project

  void apply(Project project) {
    this.project = project
    project.extensions.create("gradlerio", GradleRIOExtensions)

    String apiDest = System.getProperty("user.home") + "/wpilib/java/extracted/library/"

    project.task('roboRIO') << {
      String roboRIO = rioHost(project);
      String rioIP = rioIP(project)

      println "Host: ${roboRIO}"
      println "IP: ${rioIP}"
    }

    project.repositories.add(project.repositories.mavenCentral())

    project.getConfigurations().maybeCreate('compile')
    project.dependencies.add('compile', project.fileTree(dir: apiDest + "lib", include: "*.jar", exclude: "*-sources.jar"))

    def sshAntTask = project.getConfigurations().maybeCreate('sshAntTask')
    project.dependencies.add(sshAntTask.name, 'org.apache.ant:ant-jsch:1.7.1')
    project.dependencies.add(sshAntTask.name, 'jsch:jsch:0.1.29')

    project.ant.taskdef(name: 'scp',
    classname: 'org.apache.tools.ant.taskdefs.optional.ssh.Scp',
    classpath: sshAntTask.asPath)

    project.ant.taskdef(name: 'sshexec',
     classname: 'org.apache.tools.ant.taskdefs.optional.ssh.SSHExec',
     classpath: sshAntTask.asPath)

    def wpiTask = project.task('wpi') << {
      println "Downloading WPILib..."
      String pluginDest = System.getProperty("user.home") + "/wpilib/java/plugin/current/"
      String from = "http://first.wpi.edu/FRC/roborio/release/eclipse/plugins/edu.wpi.first.wpilib.plugins.java_0.1.0.201501011639.jar"
      download(pluginDest, from, "plugin.jar")
      println "Extracting WPILib..."

      String extractedDest = System.getProperty("user.home") + "/wpilib/java/extracted/current/"
      ant.unzip(  src:pluginDest+"plugin.jar",
      dest:extractedDest,
      overwrite:"false")
      println "WPILib Extracted..."
      println "Extracting API Resources..."
      ant.unzip(  src:extractedDest+"resources/java.zip",
      dest:apiDest,
      overwrite:"false")
      println "API Resources extracted..."
    }

    def deployTask = project.task('deploy') << {
      try {
        deploy(rioHost(project))
      } catch (Exception e) {
        println "RoboRIO not available... Falling back to IP..."
        deploy(rioIP(project))
      }
    }
    deployTask.dependsOn 'build'

    def deployIP = project.task('deployIP') << {
      deploy(rioIP(project))
    }
    deployIP.dependsOn 'build'

    def cleanRemote = project.task('cleanRIO') << {
      try {
        clean(rioHost(project))
      } catch (Exception e) {
        println "RoboRIO not available... Falling back to IP..."
        clean(rioIP(project))
      }
    }

    def cleanRemoteIP = project.task('cleanIP') << {
      clean(rioIP(project))
    }

    def reboot = project.task('reboot') << {
      try {
        reboot(rioHost(project))
      } catch (Exception e) {
        println "RoboRIO not available... Falling back to IP..."
        reboot(rioIP(project))
      }
    }

    def restartCode = project.task('restart') << {
      try {
        restartCode(rioHost(project))
      } catch (Exception e) {
        println "RoboRIO not available... Falling back to IP..."
        restartCode(rioIP(project))
      }
    } //TODO Do it after deploy

    def restartCodeIP = project.task('robotIP') << {
      restartCode(rioIP(project))
    }
  }

  void restartCode(String host) {
    println "Attempting to restart the RoboRIO code..."
    project.ant.sshexec(host: "${host}",
    username:"lvuser",
    port:22,
    trust:true,
    password:"",
    command:"/etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r"
    )
    println "Robot Code is restarting..."
  }

  void reboot(String host) {
    println "Attempting to reboot RoboRIO..."
    project.ant.sshexec(host: "${host}",
    username:"admin",
    port:22,
    trust:true,
    password:"",
    command:"reboot"
    )
    println "RoboRIO is rebooting..."
  }

  void deploy(String host) {
    println "Attempting to send new code to RoboRIO..."

    project.ant.scp(file: "build/libs/${project.name}.jar",
    todir:"lvuser@${host}:FRCUserProgram.jar",
    password:"",
    port:22,
    trust:true)

    println "Deploy Successful!"
  }

  void clean(String host) {
    println "Attempting to clean RoboRIO code..."
    project.ant.sshexec(host: "${host}",
    username:"lvuser",
    port:22,
    trust:true,
    password:"",
    command:"rm -f FRCUserProgram.jar"
    )
    println "Clean Successful!"
  }

  void download(String dest, String from, String name) {
    File output = new File(dest, name)
    if (output.exists()) {
      println "WPILib already downloaded, skipping download..."
      return
    }
    File f = new File(dest)
    f.mkdirs()
    def file = new FileOutputStream(output)
    def out = new BufferedOutputStream(file)
    out << new URL(from).openStream()
    out.close()
    println "WPILib download complete..."
  }

  String rioIP(Project project) {
    String get = project.gradlerio.rioIP
    if (get == "{DEFAULT}") {
      String team = team(project)
      int length = team.length();
      if (length < 4)
      for (int i = 0; i < 4 - length; i++)
      team = "0" + team;

      return "10." + team.substring(0, 2) + "." + team.substring(2, 4) + ".20"
    } else {
      return get
    }
  }

  String rioHost(Project project) {
    return "roboRIO-${project.gradlerio.team}.local"
  }

  String team(Project project) {
    return project.gradlerio.team
  }

}

class GradleRIOExtensions {
  String team = "0000";
  String rioIP = "{DEFAULT}";
  String robotClass = "org.usfirst.frc.team0000.Robot"
}
