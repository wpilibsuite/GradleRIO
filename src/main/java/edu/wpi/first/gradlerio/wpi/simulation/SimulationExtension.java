package edu.wpi.first.gradlerio.wpi.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.simulation.HalSimPair;
import edu.wpi.first.gradlerio.wpi.WPIPlugin;

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

    private final ArtifactView debugArtifactView;
    private final ArtifactView releaseArtifactView;

    final String desktopPlatform;
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
        filterable.include("**/*.so*", "**/*.dylib", "**/*.pdb", "**/*.dll");

        debugArtifactView = debugConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        releaseArtifactView = releaseConfiguration.getIncoming().artifactView(viewConfiguration -> {
            viewConfiguration.attributes(attributeContainer -> {
                attributeContainer.attribute(WPIPlugin.NATIVE_ARTIFACT_FORMAT,
                WPIPlugin.NATIVE_ARTIFACT_DIRECTORY_TYPE);
            });
        });

        Callable<Set<File>> debugCallable = () -> debugArtifactView.getFiles().getAsFileTree().matching(filterable).getFiles();
        Callable<Set<File>> releaseCallable = () -> releaseArtifactView.getFiles().getAsFileTree().matching(filterable).getFiles();

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

    private HalSimExtension addDep(String name, String groupId, String artifactId) {
        HalSimExtension ext = halExtensions.create(name);
        ext.getGroupId().set(groupId);
        ext.getArtifactId().set(artifactId);
        ext.getVersion().set(wpilibVersion);

        Provider<String> releaseDep = ext.getReleaseDependency(project, this);
        project.getDependencies().add(releaseConfiguration.getName(), releaseDep);
        Provider<String> debugDep = ext.getDebugDependency(project, this);
        project.getDependencies().add(debugConfiguration.getName(), debugDep);

        javaDebugList.add(debugDep);
        javaReleaseList.add(releaseDep);
        return ext;
    }

    public HalSimExtension addGui() {
        return addDep("Sim GUI", "edu.wpi.first.halsim", "halsim_gui");
    }

    public HalSimExtension addDriverstation() {
        return addDep("Sim DriverStation", "edu.wpi.first.halsim", "halsim_ds_socket");
    }

    public HalSimExtension addWebsocketsServer() {
        return addDep("Sim Websockets Server", "edu.wpi.first.halsim", "halsim_ws_server");
    }

    public HalSimExtension addWebsocketsClient() {
        return addDep("Sim Websockets Client", "edu.wpi.first.halsim", "halsim_ws_client");
    }

    public List<HalSimPair> getHalSimLocations(List<File> basePaths, boolean debug) {
        List<HalSimPair> extensions = new ArrayList<>();

        FileCollection simFiles;
        ArtifactView view;
        if (debug) {
            simFiles = debugFileCollection;
            view = debugArtifactView;
        } else {
            simFiles = releaseFileCollection;
            view = releaseArtifactView;
        }

        for (HalSimExtension halExt : getHalExtensions()) {
            Optional<String> loc = halExt.getFilenameForArtifact(view, simFiles);
            if (loc.isEmpty()) {
                continue;
            }
            for (File base : basePaths) {
                File simFile = new File(base, loc.get());
                if (simFile.exists()) {
                    extensions.add(new HalSimPair(halExt.getName(), simFile.getAbsolutePath(), halExt.getDefaultEnabled().get()));
                    break;
                }
            }

        }

        return extensions;
    }
}
