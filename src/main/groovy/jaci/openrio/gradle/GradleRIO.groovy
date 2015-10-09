package jaci.openrio.gradle;

import org.gradle.api.*;
import groovy.util.*;

class GradleRIO implements Plugin<Project> {

  def project
  String pluginDest

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

    def sshAntTask = project.getConfigurations().maybeCreate('sshAntTask')
    project.dependencies.add(sshAntTask.name, 'org.apache.ant:ant-jsch:1.7.1')
    project.dependencies.add(sshAntTask.name, 'jsch:jsch:0.1.29')

    project.ant.taskdef(name: 'scp',
    classname: 'org.apache.tools.ant.taskdefs.optional.ssh.Scp',
    classpath: sshAntTask.asPath)

    project.ant.taskdef(name: 'sshexec',
     classname: 'org.apache.tools.ant.taskdefs.optional.ssh.SSHExec',
     classpath: sshAntTask.asPath)

    WPIProvider.doDeps(project, apiDest)

    pluginDest = System.getProperty("user.home") + "/wpilib/java/plugin/current/"

    def wpiTask = project.task('wpi') << {
      String extractedDest = System.getProperty("user.home") + "/wpilib/java/extracted/current/"
      String urlBase = "http://first.wpi.edu/FRC/roborio/release/eclipse/"
      String wpiVersion = "java_0.1.0.201501221609"
      println "Checking WPILib Version..."

      String wpiInstalledVersion = ""
      try {
        def versionXML=new XmlSlurper().parse(pluginDest+"content/content.xml")
        def vNode = versionXML.depthFirst().find{it.@id == 'edu.wpi.first.wpilib.plugins.java'}
        wpiInstalledVersion = vNode.@version
        println "Currently Installed WPILib Version: ${wpiInstalledVersion}"
      } catch (Exception e) {  }

      try {
        download(pluginDest, urlBase+"content.jar", "content.jar")
        ant.unzip(src: pluginDest+"content.jar",
          dest: pluginDest+"content",
          overwrite:"true")

        def xml=new XmlSlurper().parse(pluginDest+"content/content.xml")
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
        ant.delete(dir: extractedDest)
      } catch (Exception e) {
        println "Could not check WPI Version..."
        return
      }

      String from = urlBase + "plugins/edu.wpi.first.wpilib.plugins.${wpiVersion}.jar"
      println "Downloading WPILib..."
      download(pluginDest, from, "plugin.jar")
      println "Extracting WPILib..."

      ant.unzip(src:pluginDest+"plugin.jar",
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
        restartCode(rioHost(project))
      } catch (Exception e) {
        println "RoboRIO not available... Falling back to IP..."
        deploy(rioIP(project))
        restartCode(rioIP(project))
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
    }

    def restartCodeIP = project.task('robotIP') << {
      restartCode(rioIP(project))
    }

    def setDebug = project.task('rioModeDebug') << {
      switchConfiguration('Debug', 'robotDebugCommand')
    }

    def setRun = project.task('rioModeRun') << {
      switchConfiguration('Run', 'robotCommand')
    }

    def setDebugNoHalt = project.task('rioModeHybrid') << {
      switchConfiguration('Debug (no halt)', 'robotDebugCommandNoHalt')
    }
  }

  void switchConfiguration(String type, String filename) {
    exportCaches()
    String host = rioHost(project)
    println "Switching the RoboRIO to ${type} Configuration..."
    try {
      project.ant.scp(file: "build/caches/GradleRIO/${filename}",
        todir:"lvuser@${host}:robotCommand",
        password:"",
        port:22,
        trust:true)
    } catch (Exception e) {
      println "RoboRIO not available... Falling back to IP..."
      host = rioIP(project)
      project.ant.scp(file: "build/caches/GradleRIO/${filename}",
        todir:"lvuser@${host}:robotCommand",
        password:"",
        port:22,
        trust:true)
    }
    println "RoboRIO Changed To ${type} Mode. Restarting Code Now..."
    restartCode(host)
    println "RoboRIO Code is Restarting..."
  }

  void exportCaches() {
    exportToCache("robotCommand", "robotCommand")
    exportToCache("robotDebugCommand", "robotDebugCommand")
    exportToCache("robotDebugCommandNoHalt", "robotDebugCommandNoHalt")
  }

  void exportToCache(String resource, String filename) {
    def instream = getClass().getClassLoader().getResourceAsStream("launch/" + resource)
    File dest = new File("build/caches/GradleRIO")
    dest.mkdirs()
    File file = new File(dest, filename)
    def fos = new FileOutputStream(file)
    def out = new BufferedOutputStream(fos)
    out << instream
    out.close()
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

    project.ant.scp(file: "${project.jar.archivePath}",
    todir:"lvuser@${host}:${project.gradlerio.deployFile}",
    password:"",
    port:22,
    trust:true)

    println "Deploy Successful! Loaded in: ${project.gradlerio.deployFile}"
  }

  void clean(String host) {
    println "Attempting to clean RoboRIO code..."
    project.ant.sshexec(host: "${host}",
    username:"lvuser",
    port:22,
    trust:true,
    password:"",
    command:"rm -f ${project.gradlerio.deployFile}"
    )
    println "Clean Successful!"
  }

  void download(String dest, String from, String name) {
    File output = new File(dest, name)
    File f = new File(dest)
    f.mkdirs()
    def file = new FileOutputStream(output)
    def out = new BufferedOutputStream(file)
    out << new URL(from).openStream()
    out.close()
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
  String deployFile = "FRCUserProgram.jar"
}
