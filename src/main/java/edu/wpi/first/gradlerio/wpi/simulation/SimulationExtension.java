package edu.wpi.first.gradlerio.wpi.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.wpi.WPIPlugin;
import edu.wpi.first.nativeutils.UnzipTransform;

import org.gradle.api.NamedDomainObjectContainer;

public class SimulationExtension {

    private Map<String, String> environment = new HashMap<>();

    public Map<String, String> getEnvironment() {
        return environment;
    }

    void envVar(String name, String value) {
        environment.put(name, value);
    }

    private NamedDomainObjectContainer<HalSimExtension> halExtensions;

    private final Configuration debugConfiguration;
    private final Configuration releaseConfiguration;

    public Configuration getDebugConfiguration() {
        return debugConfiguration;
    }

    public Configuration getReleaseConfiguration() {
        return releaseConfiguration;
    }

    private final SimulationDependencySet debugDependencySet;
    private final SimulationDependencySet releaseDependencySet;

    public SimulationDependencySet getDebugDependencySet() {
        return debugDependencySet;
    }

    public SimulationDependencySet getReleaseDependencySet() {
        return releaseDependencySet;
    }

    private final FileCollection debugFileCollection;
    private final FileCollection releaseFileCollection;

    public FileCollection getDebugFileCollection() {
        return debugFileCollection;
    }

    public FileCollection getReleaseFileCollection() {
        return releaseFileCollection;
    }

    private final String desktopPlatform;
    private final Provider<String> wpilibVersion;
    private final Project project;

    @Inject
    public SimulationExtension(Project project, ObjectFactory objects, ProjectLayout layout, Provider<String> wpilibVersion, String desktopPlatform) {
        halExtensions = objects.domainObjectContainer(HalSimExtension.class, name -> {
            return objects.newInstance(HalSimExtension.class, name);
        });

        this.project = project;
        this.wpilibVersion = wpilibVersion;
        this.desktopPlatform = desktopPlatform;

        javaReleaseList = new ArrayList<>();
        javaDebugList = new ArrayList<>();

        debugConfiguration = project.getConfigurations().create("simulationDebug");
        releaseConfiguration = project.getConfigurations().create("simulationRelease");

        PatternFilterable filterable = new PatternSet();
        filterable.include("**/*.so", "**/*.dylib", "**/*.pdb", "**/*.dll");

        ArtifactView debugView = debugConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        ArtifactView releaseView = releaseConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        Callable<Set<File>> debugCallable = () -> debugView.getFiles().getAsFileTree().matching(filterable).getFiles();
        Callable<Set<File>> releaseCallable = () -> releaseView.getFiles().getAsFileTree().matching(filterable).getFiles();

        debugFileCollection = project.files(debugCallable);
        releaseFileCollection = project.files(releaseCallable);

        debugDependencySet = objects.newInstance(SimulationDependencySet.class, debugFileCollection);
        releaseDependencySet = objects.newInstance(SimulationDependencySet.class, releaseFileCollection);
    }

    public NamedDomainObjectContainer<HalSimExtension> getHalExtensions() {
        return halExtensions;
    }

    public void enable(VariantComponentSpec component) {
        component.getBinaries().withType(NativeBinarySpec.class).all(binary -> {
            enable(binary);
        });
    }

    public void enable(NativeBinarySpec binary) {
        if (!binary.getTargetPlatform().getName().equals(desktopPlatform)) {
            return;
        }
        if (binary.getBuildType().getName().equalsIgnoreCase("debug")) {
            binary.lib(getDebugDependencySet());
        } else {
            binary.lib(getReleaseDependencySet());
        }
    }

    private final List<Provider<String>> javaReleaseList;
    private final List<Provider<String>> javaDebugList;

    public List<Provider<String>> enableDebug() {
        return javaDebugList;
    }

    public List<Provider<String>> enableRelease() {
        return javaReleaseList;
    }

    private void addDep(String baseName) {
        Provider<String> releaseDep = project.getProviders().provider(() -> baseName + wpilibVersion.get() + ":" + desktopPlatform + "@zip");
        project.getDependencies().add(releaseConfiguration.getName(), releaseDep);
        Provider<String> debugDep = project.getProviders().provider(() -> baseName + wpilibVersion.get() + ":" + desktopPlatform + "debug@zip");
        project.getDependencies().add(debugConfiguration.getName(), debugDep);

        javaDebugList.add(debugDep);
        javaReleaseList.add(releaseDep);
    }

    public void addGui() {
        String baseName = "edu.wpi.first.halsim:halsim_gui:";
        addDep(baseName);
    }

    public void addDriverstation() {
        String baseName = "edu.wpi.first.halsim:halsim_ds_socket:";
        addDep(baseName);
    }
}
