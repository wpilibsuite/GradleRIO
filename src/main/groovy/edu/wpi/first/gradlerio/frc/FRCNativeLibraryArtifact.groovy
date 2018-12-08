package edu.wpi.first.gradlerio.frc

import edu.wpi.first.toolchain.NativePlatforms
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

@CompileStatic
class FRCNativeLibraryArtifact extends NativeArtifact {

  FRCNativeLibraryArtifact(String name, Project project) {
    super(name, project)
    targetPlatform = NativePlatforms.roborio

    directory = FRCPlugin.LIB_DEPLOY_DIR

    postdeploy << { DeployContext ctx ->
      FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR)
      ctx.execute("ldconfig")
    }
  }

}
