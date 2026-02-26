package org.wpilib.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.gradle.api.Project;

//import org.wpilib.deployutils.deploy.artifact.NativeArtifact;

public class WPILibNativeLibraryArtifact { // TODO Extend NativeLibraryArtifact

    // @Inject
    // public WPILibNativeLibraryArtifact(String name, Project project) {
    //     super(name, project);

    //     getDirectory().set(WPILibDeployPlugin.LIB_DEPLOY_DIR);

    //     getPostdeploy().add(ctx -> {
    //         WPILibDeployPlugin.ownDirectory(ctx, WPILibDeployPlugin.LIB_DEPLOY_DIR);
    //         ctx.execute("ldconfig");
    //     });

    //     getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);

    // }

}
