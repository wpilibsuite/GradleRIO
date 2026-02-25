package org.wpilib.gradlerio.deploy.systemcore;

import javax.inject.Inject;

import org.gradle.api.Project;

//import org.wpilib.deployutils.deploy.artifact.NativeArtifact;

public class FIRSTNativeLibraryArtifact { // TODO Extend NativeLibraryArtifact

    // @Inject
    // public FIRSTNativeLibraryArtifact(String name, Project project) {
    //     super(name, project);

    //     getDirectory().set(FIRSTPlugin.LIB_DEPLOY_DIR);

    //     getPostdeploy().add(ctx -> {
    //         FIRSTPlugin.ownDirectory(ctx, FIRSTPlugin.LIB_DEPLOY_DIR);
    //         ctx.execute("ldconfig");
    //     });

    //     getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);

    // }

}
