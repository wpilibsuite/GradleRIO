package org.wpilib.gradlerio.wpi;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativeplatform.plugins.NativeComponentPlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;
import org.wpilib.gradlerio.wpi.cpp.WPINativeExtension;
import org.wpilib.gradlerio.wpi.java.WPIJavaExtension;
import org.wpilib.gradlerio.wpi.simulation.SimulationExtension;
import org.wpilib.nativeutils.vendordeps.WPIVendorDepsExtension;
import org.wpilib.nativeutils.vendordeps.WPIVendorDepsPlugin;
import org.wpilib.toolchain.NativePlatforms;

public class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs

    private static final List<String> validImageVersions = List.of();

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities

    private final WPIMavenExtension maven;
    private final SimulationExtension sim;

    private Property<String> wpilibYear;

    private final NativePlatforms platforms;

    private final Project project;
    private final String cppToolsClassifier;

    private final WPIVendorDepsExtension vendor;

    public WPIVendorDepsExtension getVendor() {
        return vendor;
    }

    public SimulationExtension getSim() {
        return sim;
    }

    @Inject
    public WPIExtension(Project project) {
        this.project = project;
        ObjectFactory factory = project.getObjects();
        platforms = new NativePlatforms();

        wpilibYear = factory.property(String.class);
        wpilibYear.convention("2027_alpha5");

        wpilibHome = factory.directoryProperty().fileProvider(project.provider(WPIExtension::computeHomeRoot))
                .dir(wpilibYear);

        versions = factory.newInstance(WPIVersionsExtension.class);

        project.getPlugins().apply(WPIVendorDepsPlugin.class);
        vendor = project.getExtensions().getByType(WPIVendorDepsExtension.class);
        vendor.getFixedVersion().set(versions.getWpilibVersion());
        vendor.getWpilibYear().set(wpilibYear);
        vendor.getWpilibHome().set(wpilibHome);

        // TODO in the future make this lazy
        vendor.loadAll();
        vendor.validateDependencies();

        sim = factory.newInstance(SimulationExtension.class, project, versions.getWpilibVersion(),
                NativePlatforms.desktop);

        project.getPlugins().withType(NativeComponentPlugin.class, p -> {
            cpp = factory.newInstance(WPINativeExtension.class, project, this, versions);
            vendor.getNativeVendor().initializeNativeDependencies();
        });

        project.getPlugins().withType(JavaPlugin.class, p -> {
            java = factory.newInstance(WPIJavaExtension.class, project, sim, versions);

            project.getPluginManager().apply(EclipsePlugin.class);
            EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
            EclipseClasspath eclipseClasspath = eclipse.getClasspath();
            eclipseClasspath.containers("org.eclipse.buildship.core.gradleclasspathcontainer");
            eclipseClasspath.getFile().whenMerged(cp -> {
                if (cp instanceof org.gradle.plugins.ide.eclipse.model.Classpath ecp) {
                    List<ClasspathEntry> entries = ecp.getEntries();
                    // TODO make this grab the build folder dynamically, and include everything else necessary
                    SourceFolder src = new SourceFolder("build/generated/sources/annotationProcessor/java/main", null);
                    entries.add(src);
                }
            });

        });

        maven = factory.newInstance(WPIMavenExtension.class, project);

        if (project.hasProperty("forceCppToolsClassifier")) {
            this.cppToolsClassifier = (String) project.findProperty("forceCppToolsClassifier");
        } else {
            this.cppToolsClassifier = NativePlatforms.desktopOS() + NativePlatforms.desktopArchDirect();
        }
    }

    private final Provider<Directory> wpilibHome;

    public Provider<Directory> getWpilibHome() {
        return wpilibHome;
    }

    private static File computeHomeRoot() {
        File homeRoot = null;
        if (OperatingSystem.current().isWindows()) {
            String publicFolder = System.getenv("PUBLIC");
            if (publicFolder == null) {
                publicFolder = "C:\\Users\\Public";
            }
            homeRoot = new File(publicFolder, "wpilib");
        } else {
            String userFolder = System.getProperty("user.home");
            homeRoot = new File(userFolder, "wpilib");
        }
        return homeRoot;
    }

    public static List<String> getValidImageVersions() {
        return validImageVersions;
    }

    private final WPIVersionsExtension versions;

    public WPIVersionsExtension getVersions() {
        return versions;
    }

    private WPINativeExtension cpp;

    public WPINativeExtension getCpp() {
        return cpp;
    }

    private WPIJavaExtension java;

    public WPIJavaExtension getJava() {
        return java;
    }

    public WPIMavenExtension getMaven() {
        return maven;
    }

    public Property<String> getWpilibYear() {
        return wpilibYear;
    }

    public NativePlatforms getPlatforms() {
        return platforms;
    }

    public Project getProject() {
        return project;
    }

    public String getCppToolsClassifier() {
        return cppToolsClassifier;
    }
}
