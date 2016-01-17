package jaci.openrio.gradle;

import org.gradle.api.*;
import groovy.util.*;

class GradleRIO implements Plugin<Project> {

  public static def project

  void apply(Project project) {
    GradleRIO.project = project
    project.extensions.create("gradlerio", GradleRIOExtensions)

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

    WPIProvider.init(project)

    def deployTask = project.task('deploy') << { }
    
    deployTask.doFirst {
      tryOnAll(project) {
        deploy(it, project.jar.archivePath, project.gradlerio.deployFile)
        switchConfiguration(it, 'Run', 'robotCommand')
        restartCode(it)
        sanityCheck(it)
      }
    }
    
    deployTask.dependsOn 'build'
    deployTask.setDescription "Build and Deploy this code to the RoboRIO and restart the robot code."

    def cleanRemote = project.task('cleanRIO') << {
      tryOnAll(project) {
        clean(it)
      }
    }
    cleanRemote.setDescription "Remove your code from the RoboRIO"

    def reboot = project.task('reboot') << {
      tryOnAll(project) {
        reboot(it)
      }
    }
    reboot.setDescription "Reboot the RoboRIO"

    def restartCode = project.task('restart') << {
      tryOnAll(project) {
        restartCode(it)
      }
    }
    restartCode.setDescription "Restart the RoboRIO's Robot Code"

    def setDebug = project.task('rioModeDebug') << {
      switchConfiguration('Debug', 'robotDebugCommand')
    }
    setDebug.setDescription "Change the RoboRIO's Run Configuration to Debug Mode (must attach remote debugger to start code)"

    def setRun = project.task('rioModeRun') << {
      switchConfiguration('Run', 'robotCommand')
    }
    setRun.setDescription "Change the RoboRIO's Run Configuration to Run Mode (default)"

    def setDebugNoHalt = project.task('rioModeHybrid') << {
      switchConfiguration('Debug (no halt)', 'robotDebugCommandNoHalt')
    }
    setDebugNoHalt.setDescription "Change the RoboRIO's Run Configuration to Hybrid Mode (debugger accessible, but not required)"
  }
  
  void sanityCheck(String host) {
    try {
      project.ant.sshexec(host: "${it}",
        username:"lvuser",
        port:22,
        trust:true,
        password:"",
        command:"/usr/local/frc/JRE/bin/java -version"
      )
    } catch (Throwable t) {
      println "==== WARNING: Java is not installed on the RoboRIO! The files have been deployed successfully, but will not be able to be launched until Java is installed! ===="
    }
  }

  void switchConfiguration(String type, String filename) {
    println "Switching the RoboRIO to ${type} Configuration..."

    tryOnAll(project) {
      host = it
      switchConfiguration(host, type, filename)
    }

    println "RoboRIO Changed To ${type} Mode. Restarting Code Now..."
    restartCode(host)
    println "RoboRIO Code is Restarting..."
  }
  
  void switchConfiguration(String host, String type, String filename) {
    String file_main = exportCaches()
    project.ant.scp(file: "${file_main}/${filename}",
      todir:"lvuser@${it}:robotCommand",
      password:"",
      port:22,
      trust:true)
        
    project.ant.sshexec(host: "${it}",
      username:"lvuser",
      port:22,
      trust:true,
      password:"",
      command:"chmod +x /home/lvuser/robotCommand"
    )
  }

  static String exportCaches() {
    exportToCache("launch/robotCommand", "robotCommand")
    exportToCache("launch/robotDebugCommand", "robotDebugCommand")
    exportToCache("launch/robotDebugCommandNoHalt", "robotDebugCommandNoHalt")
    exportToCache("toast/nashorn.jar", "nashorn.jar")
    return new File("build/caches/GradleRIO").getAbsolutePath();
  }

  static void exportToCache(String resource, String filename) {
    def instream = GradleRIO.class.getClassLoader().getResourceAsStream(resource)
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
      failonerror:false,
      command:"killall -q netconsole-host || :"
    )
    
    project.ant.sshexec(host: "${host}",
      username:"lvuser",
      port:22,
      trust:true,
      password:"",
      failonerror:false,
      command:". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r"
    )
    
    project.ant.sshexec(host: "${host}",
      username:"lvuser",
      port:22,
      trust:true,
      password:"",
      command:"sync"
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

  void deploy(String host, File inFile, String outFile) {
    println "Attempting to send new code to RoboRIO..."

    if (WPIProvider.isToast()) ToastDeploy.mapToastDeps(project)

    project.ant.scp(file: inFile.getAbsolutePath(),
      todir:"lvuser@${host}:${outFile}",
      password:"",
      port:22,
      trust:true)

    for (def deployer : project.gradlerio.deployers) {
      def from = deployer.from
      def to = deployer.to
      def user = deployer.elevated == null ? "lvuser" : "admin"
      project.ant.scp(file: from,
        todir: "${user}@${host}:${to}",
        password: "",
        port:22,
        trust:true)
    }

    println "Deploy Successful! Loaded in: ${outFile}"
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

  public static void download(String dest, String from, String name) {
    File output = new File(dest, name)
    File f = new File(dest)
    f.mkdirs()
    def file = new FileOutputStream(output)
    def out = new BufferedOutputStream(file)
    out << new URL(from).openStream()
    out.close()
  }

  static void tryOnAll(Project project, Closure action) {
    try {
      def host = rioHost(project)
      println "Attempting via Hostname ${host}..."
      action.call(host)
    } catch (Throwable e1) {
      try {
        println "Hostname failed. Trying on USB Interface..."
        action.call(rioUSB(project))
      } catch (Throwable e2) {
        def ip = rioIP(project)
        println "Hostname failed. Attempting via IP ${ip}..."
        action.call(ip)
      }
    }
  }

  static String rioUSB(Project project) {
    return "172.22.11.2"
  }

  static String rioIP(Project project) {
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

  static String rioHost(Project project) {
    return "roboRIO-${project.gradlerio.team}-frc.local"
  }

  static String team(Project project) {
    return project.gradlerio.team
  }

}
