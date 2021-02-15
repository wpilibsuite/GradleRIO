package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.deployutils.deploy.artifact.NativeArtifact;

public class FRCNativeLibraryArtifact extends NativeArtifact {

    @Inject
    public FRCNativeLibraryArtifact(String name, Project project) {
        super(name, project);

        getDirectory().set(FRCPlugin.LIB_DEPLOY_DIR);

        getPostdeploy().add(ctx -> {
            FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR);
            ctx.execute("ldconfig");
        });

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);

    }

}
