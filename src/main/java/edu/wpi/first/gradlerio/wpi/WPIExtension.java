package edu.wpi.first.gradlerio.wpi;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.wpi.dependencies.WPIDepsExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.NativePlatforms;

public class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    private final Property<String> wpilibVersion;
    private String niLibrariesVersion = "2020.10.1";
    private String opencvVersion = "3.4.7-5";
    private String imguiVersion = "1.79-2";
    private String ejmlVersion = "0.38";
    private String jacksonVersion = "2.10.0";
    private String wpimathVersion = "2021.2.2";
    private static final List<String> validImageVersions = List.of("2020_v10", "2021_v1", "2021_v2", "2021_v3.*");

    private String googleTestVersion = "1.9.0-5-437e100-1";

    private String jreArtifactLocation = "edu.wpi.first.jdk:roborio-2021:11.0.9u11-1";

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    private String smartDashboardVersion = "2021.2.2";
    private String shuffleboardVersion = "2021.2.2";
    private String outlineViewerVersion = "2021.2.2";
    private String robotBuilderVersion = "2021.2.2";
    private String robotBuilderOldVersion = "2021.2.2";
    private String pathWeaverVersion = "2021.2.2";
    private String glassVersion = "2021.2.2";

    private final WPIMavenExtension maven;
    private final WPIDepsExtension deps;
    private final SimulationExtension sim;

    private String frcYear = "2021";

    private final NativePlatforms platforms;

    private final Project project;
    private final String toolsClassifier;
    private final String cppToolsClassifier;

    private NativeUtilsExtension ntExt;

    public SimulationExtension getSim() {
        return sim;
    }

    @Inject
    public WPIExtension(Project project) {
        this.project = project;
        ObjectFactory factory = project.getObjects();
        wpilibVersion = factory.property(String.class);
        // TODO clean up so defaults are better
        wpilibVersion.set("2021.2.2");
        platforms = new NativePlatforms();
        maven = factory.newInstance(WPIMavenExtension.class, project);
        sim = factory.newInstance(SimulationExtension.class, project, wpilibVersion, NativePlatforms.desktop);

        if (project.hasProperty("forceToolsClassifier")) {
            this.toolsClassifier = (String)project.findProperty("forceToolsClassifier");
        } else {
            this.toolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch") == "amd64" ? "win64" : "win32" :
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
                            System.getProperty("os.arch") == "amd64" ? "windowsx86-64" : "windowsx86" :
                            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                            null
            );
        }


        deps = factory.newInstance(WPIDepsExtension.class, project, this);
    }

    public void maven(final Action<? super WPIMavenExtension> closure) {
      closure.execute(maven);
    }

    public void useLibrary(VariantComponentSpec component, String... libraries) {
        useRequiredLibrary(component, libraries);
    }

    public void useLibrary(NativeBinarySpec binary, String... libraries) {
        useRequiredLibrary(binary, libraries);
    }

    public void useRequiredLibrary(VariantComponentSpec component, String... libraries) {
        if (ntExt == null) {
            ntExt = project.getExtensions().getByType(NativeUtilsExtension.class);
        }
        ntExt.useRequiredLibrary(component, libraries);
    }

    public void useRequiredLibrary(NativeBinarySpec binary, String... libraries) {
        if (ntExt == null) {
            ntExt = project.getExtensions().getByType(NativeUtilsExtension.class);
        }
        ntExt.useRequiredLibrary(binary, libraries);
    }

    public void useOptionalLibrary(VariantComponentSpec component, String... libraries) {
        if (ntExt == null) {
            ntExt = project.getExtensions().getByType(NativeUtilsExtension.class);
        }
        ntExt.useOptionalLibrary(component, libraries);
    }

    public void useOptionalLibrary(NativeBinarySpec binary, String... libraries) {
        if (ntExt == null) {
            ntExt = project.getExtensions().getByType(NativeUtilsExtension.class);
        }
        ntExt.useOptionalLibrary(binary, libraries);
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

    public Property<String> getWpilibVersion() {
        return wpilibVersion;
    }

    public String getNiLibrariesVersion() {
        return niLibrariesVersion;
    }

    public String getOpencvVersion() {
        return opencvVersion;
    }

    public String getImguiVersion() {
        return imguiVersion;
    }

    public String getEjmlVersion() {
        return ejmlVersion;
    }

    public String getJacksonVersion() {
        return jacksonVersion;
    }

    public String getWpimathVersion() {
        return wpimathVersion;
    }

    public static List<String> getValidImageVersions() {
        return validImageVersions;
    }

    public String getGoogleTestVersion() {
        return googleTestVersion;
    }

    public String getJreArtifactLocation() {
        return jreArtifactLocation;
    }

    public String getSmartDashboardVersion() {
        return smartDashboardVersion;
    }

    public String getShuffleboardVersion() {
        return shuffleboardVersion;
    }

    public String getOutlineViewerVersion() {
        return outlineViewerVersion;
    }

    public String getRobotBuilderVersion() {
        return robotBuilderVersion;
    }

    public String getRobotBuilderOldVersion() {
        return robotBuilderOldVersion;
    }

    public String getPathWeaverVersion() {
        return pathWeaverVersion;
    }

    public String getGlassVersion() {
        return glassVersion;
    }

    public WPIMavenExtension getMaven() {
        return maven;
    }

    public WPIDepsExtension getDeps() {
        return deps;
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

    public NativeUtilsExtension getNtExt() {
        return ntExt;
    }

    public void setNiLibrariesVersion(String niLibrariesVersion) {
        this.niLibrariesVersion = niLibrariesVersion;
    }

    public void setOpencvVersion(String opencvVersion) {
        this.opencvVersion = opencvVersion;
    }

    public void setImguiVersion(String imguiVersion) {
        this.imguiVersion = imguiVersion;
    }

    public void setEjmlVersion(String ejmlVersion) {
        this.ejmlVersion = ejmlVersion;
    }

    public void setJacksonVersion(String jacksonVersion) {
        this.jacksonVersion = jacksonVersion;
    }

    public void setWpimathVersion(String wpimathVersion) {
        this.wpimathVersion = wpimathVersion;
    }

    public void setGoogleTestVersion(String googleTestVersion) {
        this.googleTestVersion = googleTestVersion;
    }

    public void setJreArtifactLocation(String jreArtifactLocation) {
        this.jreArtifactLocation = jreArtifactLocation;
    }

    public void setSmartDashboardVersion(String smartDashboardVersion) {
        this.smartDashboardVersion = smartDashboardVersion;
    }

    public void setShuffleboardVersion(String shuffleboardVersion) {
        this.shuffleboardVersion = shuffleboardVersion;
    }

    public void setOutlineViewerVersion(String outlineViewerVersion) {
        this.outlineViewerVersion = outlineViewerVersion;
    }

    public void setRobotBuilderVersion(String robotBuilderVersion) {
        this.robotBuilderVersion = robotBuilderVersion;
    }

    public void setRobotBuilderOldVersion(String robotBuilderOldVersion) {
        this.robotBuilderOldVersion = robotBuilderOldVersion;
    }

    public void setPathWeaverVersion(String pathWeaverVersion) {
        this.pathWeaverVersion = pathWeaverVersion;
    }

    public void setGlassVersion(String glassVersion) {
        this.glassVersion = glassVersion;
    }

    public void setFrcYear(String frcYear) {
        this.frcYear = frcYear;
    }
}
