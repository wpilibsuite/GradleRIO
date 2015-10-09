package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;
import groovy.util.*;

public class ToastIDE {

  static void init(Project project) {
    project.task('intelliJLaunchConfig') << {
      intelliJ_run_config(project, "Sim", "-sim --search -ide IDEA")
      intelliJ_run_config(project, "Verify", "-verify -sim --search -ide IDEA")
      intelliJ_remote_config(project, "Remote", "roborio-${project.gradlerio.team}.local")
      intelliJ_gradle_config(project, "Deploy", "deploy")
    }

    project.task('eclipseLaunchConfig') << {
      eclipse_run_config(project, "Sim", "-sim --search --no-color -ide ECLIPSE")
      eclipse_run_config(project, "Verify", "-verify -sim --search --no-color -ide ECLIPSE")
      eclipse_remote_config(project, "Remote", "roborio-${project.gradlerio.team}.local")
    }

    for (def task : project.getTasksByName('eclipseClasspath', false)) {
      task.dependsOn('eclipseLaunchConfig')
    }
    for (def task : project.getTasksByName('ideaModule', false)) {
      task.dependsOn('intelliJLaunchConfig')
    }
  }

  static void intelliJ_run_config(Project project, String name, String params) {
    project.idea.workspace.iws.withXml { XmlProvider provider ->
      def node = provider.node
      node.depthFirst().each {
				if (it.name() == "component" && it.attribute("name") == "RunManager") {
          def config = it.appendNode("configuration", [default: false, factoryName: "Application", type: "Application", name: "${project.archivesBaseName}-${name}", folderName: "Launch"])
          config.appendNode("option", [name: "MAIN_CLASS_NAME", value: "jaci.openrio.toast.core.ToastBootstrap"])
					config.appendNode("option", [name: "WORKING_DIRECTORY", value: "file://\$PROJECT_DIR\$/run"])
          config.appendNode("option", [name: "PROGRAM_PARAMETERS", value: params])
					config.appendNode("module", [name: "${project.archivesBaseName}"])
        }
      }
    }
  }

  static void intelliJ_remote_config(Project project, String name, String address) {
    project.idea.workspace.iws.withXml { XmlProvider provider ->
      def node = provider.node
      node.depthFirst().each {
        if (it.name() == "component" && it.attribute("name") == "RunManager") {
          def config = it.appendNode("configuration", [default: "false", factoryName: "Remote", type: "Remote", name: "${project.archivesBaseName}-${name}", folderName: "Remote"])
      		config.appendNode("option", [name: "HOST", value: address])
      		config.appendNode("option", [name: "PORT", value: "5910"])
      		config.appendNode("option", [name: "USE_SOCKET_TRANSPORT", value: "true"])
      		config.appendNode("module", [name: "${project.archivesBaseName}"])
        }
      }
    }
  }

  static void intelliJ_gradle_config(Project project, String name, String gradle_task) {
    project.idea.workspace.iws.withXml { XmlProvider provider ->
      def node = provider.node
      node.depthFirst().each {
        if (it.name() == "component" && it.attribute("name") == "RunManager") {
          def config = it.appendNode("configuration", [default: "false", factoryName: "Gradle", type: "GradleRunConfiguration", name: "${project.archivesBaseName}-${name}", folderName: "Gradle"])
					config = config.appendNode("ExternalSystemSettings")
					config.appendNode("option", [name: "externalProjectPath", value: "\$PROJECT_DIR\$/build.gradle"])
					config.appendNode("option", [name: "externalSystemIdString", value: "GRADLE"])
					config = config.appendNode("option", [name: "taskNames"])
					config.appendNode("list").appendNode("option", [value: gradle_task])
        }
      }
    }
  }

  static void eclipse_run_config(Project project, String name, String param) {
    new File("gradle/.launch").mkdir()
    def writer = new FileWriter(project.file("gradle/.launch/${project.archivesBaseName}-${name}.launch"))
    def xml = new groovy.xml.MarkupBuilder(writer).launchConfiguration("type": "org.eclipse.jdt.launching.localJavaApplication") {
      stringAttribute("key": "org.eclipse.jdt.launching.MAIN_TYPE", "value": "jaci.openrio.toast.core.ToastBootstrap")
      stringAttribute("key": "org.eclipse.jdt.launching.PROJECT_ATTR", "value": project.name)
      stringAttribute("key": "org.eclipse.jdt.launching.WORKING_DIRECTORY", "value": project.file("run"))
      stringAttribute("key": "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS", "value": param)
    }
    writer.close()
  }

  static void eclipse_remote_config(Project project, String name, String address) {
    new File("gradle/.launch").mkdir()
    def writer = new FileWriter(project.file("gradle/.launch/${project.archivesBaseName}-${name}.launch"))
    def xml = new groovy.xml.MarkupBuilder(writer).launchConfiguration("type": "org.eclipse.jdt.launching.remoteJavaApplication") {
      stringAttribute("key": "org.eclipse.jdt.launching.VM_CONNECTOR_ID", "value": "org.eclipse.jdt.launching.socketAttachConnector")
      stringAttribute("key": "org.eclipse.jdt.launching.PROJECT_ATTR", "value": project.name)
      mapAttribute("key": "org.eclipse.jdt.launching.CONNECT_MAP") {
        mapEntry("key": "hostname", "value": address)
        mapEntry("key": "port", "value": "5910")
      }
    }
    writer.close()
  }

}
