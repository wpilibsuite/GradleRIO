package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import edu.wpi.first.deployutils.deploy.artifact.FileCollectionArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.gradlerio.deploy.FRCDeployPlugin;

public class FRCJNILibraryArtifact extends FileCollectionArtifact {
    private Property<Configuration> configuration;
    private boolean zipped;
    private PatternFilterable filter;

    @Inject
    public FRCJNILibraryArtifact(String name, RemoteTarget target) {
        super(name, target);

        getDirectory().set(FRCDeployPlugin.LIB_DEPLOY_DIR);

        filter = new PatternSet();

        configuration = target.getProject().getObjects().property(Configuration.class);

        setOnlyIf(ctx -> {
            return getFiles().isPresent() && !getFiles().get().isEmpty() && !getFiles().get().getFiles().isEmpty();
        });

        getPreWorkerThread().add(cfg -> {
            if (!configuration.isPresent()) {
                return;
            }
            getFiles().set(computeFiles());

        });

        getPostdeploy().add(ctx -> {
            FRCDeployPlugin.ownDirectory(ctx, FRCDeployPlugin.LIB_DEPLOY_DIR);
            ctx.execute("ldconfig");
        });
    }

    public Property<Configuration> getConfiguration() {
        return configuration;
    }

    public boolean isZipped() {
        return zipped;
    }

    public void setZipped(boolean zipped) {
        this.zipped = zipped;
    }

    public PatternFilterable getFilter() {
        return filter;
    }

    @Override
    public void deploy(DeployContext arg0) {

        super.deploy(arg0);
    }

    public FileCollection computeFiles() {
        Set<File> configFileCaches = configuration.get().getResolvedConfiguration().getFiles();
        if (zipped) {
            Optional<FileTree> allFiles = configFileCaches.stream().map(file -> getTarget().getProject().zipTree(file).matching(filter)).filter(x -> x != null).reduce((a, b) -> a.plus(b));
            if (allFiles.isPresent()) {
                return allFiles.get();
            } else {
                return getTarget().getProject().files();
            }
        } else {
            return getTarget().getProject().files(configFileCaches);
        }
    }

}
