package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import org.gradle.api.Project;

//import edu.wpi.first.deployutils.deploy.artifact.NativeArtifact;

public class FRCNativeLibraryArtifact { // TODO Extend NativeLibraryArtifact

    // @Inject
    // public FRCNativeLibraryArtifact(String name, Project project) {
    //     super(name, project);

    //     getDirectory().set(FRCPlugin.LIB_DEPLOY_DIR);

    //     getPostdeploy().add(ctx -> {
    //         FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR);
    //         ctx.execute("ldconfig");
    //     });

    //     getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);

    // }

}
