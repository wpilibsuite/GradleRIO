package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

@CompileStatic
class FRCNativeLibraryArtifact extends NativeArtifact {

  FRCNativeLibraryArtifact(String name, Project project) {
    super(name, project)
    targetPlatform = 'roborio'

    directory = '/usr/local/frc/lib'
    postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }
  }

}
