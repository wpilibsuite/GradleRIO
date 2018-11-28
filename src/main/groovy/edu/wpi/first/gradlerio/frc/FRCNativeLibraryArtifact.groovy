package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

@CompileStatic
class FRCNativeLibraryArtifact extends NativeArtifact {

  FRCNativeLibraryArtifact(String name, Project project) {
    super(name, project)
    targetPlatform = WPIExtension.Platforms.roborio

    directory = '/usr/local/frc/lib'
    postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }
  }

}
