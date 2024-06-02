package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import org.gradle.api.provider.Property;

import edu.wpi.first.deployutils.deploy.artifact.AbstractArtifact;
import edu.wpi.first.deployutils.deploy.cache.CacheMethod;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.FRCExtension;
import edu.wpi.first.gradlerio.deploy.StagedDeployTarget;

public class ClearFilesArtifact extends AbstractArtifact {
    @Inject
    public ClearFilesArtifact(String name, StagedDeployTarget target) {
        super(name, target);

        target.setDeployStage(this, DeployStage.FileClear);
    }

    @Override
    public void deploy(DeployContext context) {
        if (getTarget().getProject().getExtensions().getByType(FRCExtension.class).isClearFiles()) {
            context.execute("rm -rf /home/lvuser/*");
        }
    }
}
