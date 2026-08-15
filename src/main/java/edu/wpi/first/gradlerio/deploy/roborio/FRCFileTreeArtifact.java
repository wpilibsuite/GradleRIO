package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import edu.wpi.first.deployutils.deploy.artifact.FileTreeArtifact;

public class FRCFileTreeArtifact extends FileTreeArtifact {
    @Inject
    FRCFileTreeArtifact(String name, RoboRIO target) {
        super(name, target);

        getPostdeploy().add(ctx -> {
            ctx.execute("chown lvuser \"" + getFiles().get().toString() + "\"");
        });
    }
}
