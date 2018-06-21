package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.artifact.NativeArtifact

import java.nio.file.Paths

@CompileStatic
class FRCNativeLibraryArtifact extends NativeArtifact {
  FRCNativeLibraryArtifact(String name) {
    super(name)
    targetPlatform = 'roborio'

    directory = '/usr/local/frc/lib'
    postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }
  }
}
