package edu.wpi.first.gradlerio.wpi;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativeplatform.plugins.NativeComponentPlugin;

import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
import edu.wpi.first.gradlerio.wpi.java.WPIJavaExtension;
import edu.wpi.first.gradlerio.wpi.cpp.WPINativeExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;
import edu.wpi.first.toolchain.NativePlatforms;

public class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs

    private static final List<String> validImageVersions = List.of("2022_v4.*");

    private String jreArtifactLocation = "edu.wpi.first.jdk:roborio-2021:11.0.9u11-1";

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities

    private final WPIMavenExtension maven;
    private final SimulationExtension sim;

    private String frcYear = "2022";

    private final NativePlatforms platforms;

    private final Project project;
    private final String toolsClassifier;
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

        versions = factory.newInstance(WPIVersionsExtension.class);

        vendor = project.getObjects().newInstance(WPIVendorDepsExtension.class, this);
        sim = factory.newInstance(SimulationExtension.class, project, versions.getWpilibVersion(), NativePlatforms.desktop);

        project.getPlugins().withType(NativeComponentPlugin.class, p -> {
            cpp = factory.newInstance(WPINativeExtension.class, project, vendor, versions);
        });

        project.getPlugins().withType(JavaPlugin.class, p -> {
            java = factory.newInstance(WPIJavaExtension.class, project, sim, versions, vendor);
        });

        maven = factory.newInstance(WPIMavenExtension.class, project);

        if (project.hasProperty("forceToolsClassifier")) {
            this.toolsClassifier = (String)project.findProperty("forceToolsClassifier");
        } else {
            this.toolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch").equals("amd64") ? "win64" : "win32" :
                            OperatingSystem.current().isMacOsX() ? "mac64" :
                                    OperatingSystem.current().isLinux() ? "linux64" :
                                            null
            );
        }
        if (project.hasProperty("forceCppToolsClassifier")) {
            this.cppToolsClassifier = (String)project.findProperty("forceCppToolsClassifier");
        } else {
            this.cppToolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch").equals("amd64") ? "windowsx86-64" : "windowsx86" :
                            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                            null
            );
        }
    }

    private String frcHomeCache;

    public String getFrcHome() {
        if (frcHomeCache != null) {
            return this.frcHomeCache;
        }
        String frcHome = "";
        if (OperatingSystem.current().isWindows()) {
            String publicFolder = System.getenv("PUBLIC");
            if (publicFolder == null) {
                publicFolder = "C:\\Users\\Public";
            }
            File homeRoot = new File(publicFolder, "wpilib");
            frcHome = new File(homeRoot, this.frcYear).toString();
        } else {
            String userFolder = System.getProperty("user.home");
            File homeRoot = new File(userFolder, "wpilib");
            frcHome = new File(homeRoot, this.frcYear).toString();
        }
        frcHomeCache = frcHome;
        return frcHomeCache;
    }

    // public Map<String, Tuple> versions() {
    //     // Format:
    //     // property: [ PrettyName, Version, RecommendedKey ]
    //     return [
    //             "wpilibVersion"        : new Tuple("WPILib", wpilibVersion, "wpilib"),
    //             "opencvVersion"        : new Tuple("OpenCV", opencvVersion, "opencv"),
    //             "frcYear   "           : new Tuple("FRC Year", frcYear, "frcYear"),
    //             "googleTestVersion"    : new Tuple("Google Test", googleTestVersion, "googleTest"),
    //             "imguiVersion"         : new Tuple("ImGUI", imguiVersion, "imgui"),
    //             "wpimathVersion"       : new Tuple("WPIMath", wpimathVersion, "wpimath"),
    //             "ejmlVersion"          : new Tuple("EJML", ejmlVersion, "ejml"),
    //             "jacksonVersion"       : new Tuple("Jackson", jacksonVersion, "jackson"),

    //             "smartDashboardVersion": new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
    //             "shuffleboardVersion"  : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),
    //             "outlineViewerVersion" : new Tuple("OutlineViewer", outlineViewerVersion, "outlineviewer"),
    //             "robotBuilderVersion"  : new Tuple("RobotBuilder", robotBuilderVersion, "robotbuilder"),
    //             "RobotBuilderOldVersion"  : new Tuple("RobotBuilder-Old", robotBuilderOldVersion, "robotbuilderold"),
    //             "glassVersion"         : new Tuple("Glass", glassVersion, "glass"),
    //             "pathWeaverVersion"    : new Tuple("PathWeaver", pathWeaverVersion, "pathweaver"),
    //     ]
    // }



    public static List<String> getValidImageVersions() {
        return validImageVersions;
    }

    public String getJreArtifactLocation() {
        return jreArtifactLocation;
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

    public String getFrcYear() {
        return frcYear;
    }

    public NativePlatforms getPlatforms() {
        return platforms;
    }

    public Project getProject() {
        return project;
    }

    public String getToolsClassifier() {
        return toolsClassifier;
    }

    public String getCppToolsClassifier() {
        return cppToolsClassifier;
    }

    public void setJreArtifactLocation(String jreArtifactLocation) {
        this.jreArtifactLocation = jreArtifactLocation;
    }

    public void setFrcYear(String frcYear) {
        this.frcYear = frcYear;
    }
}
