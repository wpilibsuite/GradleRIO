package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.util.PatternFilterable;

import edu.wpi.first.embeddedtools.deploy.artifact.FileCollectionArtifact;

public class FRCJNILibraryArtifact extends FileCollectionArtifact implements Callable<FileCollection> {
    private Configuration configuration;
    private boolean zipped;
    private Action<PatternFilterable> filter;
    private Set<File> configFileCaches;

    @Inject
    public FRCJNILibraryArtifact(String name, Project project) {
        super(name, project);

        getDirectory().set(FRCPlugin.LIB_DEPLOY_DIR);

        setOnlyIf(ctx -> {
            return getFiles().isPresent() && !getFiles().get().isEmpty() && !getFiles().get().getFiles().isEmpty();
        });

        getPostdeploy().add(ctx -> {
            FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR);
            ctx.execute("ldconfig");
        });

        getFiles().set(project.files(this));
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isZipped() {
        return zipped;
    }

    public void setZipped(boolean zipped) {
        this.zipped = zipped;
    }

    public Action<PatternFilterable> getFilter() {
        return filter;
    }

    public void setFilter(Action<PatternFilterable> filter) {
        this.filter = filter;
    }

    public Set<File> getConfigFileCaches() {
        return configFileCaches;
    }

    public void setConfigFileCaches(Set<File> configFileCaches) {
        this.configFileCaches = configFileCaches;
    }

    @Override
    public FileCollection call() {
        if (configFileCaches == null) {
            configFileCaches = configuration.getResolvedConfiguration().getFiles();
        }
        if (zipped) {
            Optional<FileTree> allFiles = configFileCaches.stream().map(file -> getProject().zipTree(file).matching(filter)).filter(x -> x != null).reduce((a, b) -> a.plus(b));
            if (allFiles.isPresent()) {
                return allFiles.get();
            } else {
                return getProject().files();
            }
        } else {
            return getProject().files(configFileCaches);
        }
    }

}
