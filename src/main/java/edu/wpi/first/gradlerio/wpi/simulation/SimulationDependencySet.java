package edu.wpi.first.gradlerio.wpi.simulation;

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.nativeplatform.NativeDependencySet;

public class SimulationDependencySet implements NativeDependencySet {

    private final FileCollection emptyCollection;
    private final FileCollection runtimeFiles;

    @Inject
    public SimulationDependencySet(ProjectLayout layout, FileCollection runtimeFiles) {
        emptyCollection = layout.files();

        this.runtimeFiles = runtimeFiles;
    }

    @Override
    public FileCollection getIncludeRoots() {
        return emptyCollection;
    }

    @Override
    public FileCollection getLinkFiles() {
        return emptyCollection;
    }

    @Override
    public FileCollection getRuntimeFiles() {
        return runtimeFiles;
    }

}
