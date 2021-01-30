package edu.wpi.first.gradlerio.wpi

import edu.wpi.first.gradlerio.wpi.dependencies.WPIDepsExtension
import edu.wpi.first.toolchain.NativePlatforms
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem
import edu.wpi.first.nativeutils.NativeUtilsExtension
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.VariantComponentSpec

import javax.inject.Inject

@CompileStatic
class WPIExtension {
    // WPILib (first.wpi.edu/FRC/roborio/maven) libs
    String wpilibVersion = "2021.2.2"
    String niLibrariesVersion = "2020.10.1"
    String opencvVersion = "3.4.7-5"
    String imguiVersion = "1.79-2"
    String ejmlVersion = "0.38"
    String jacksonVersion = "2.10.0"
    String wpimathVersion = "2021.2.2"
    static final String[] validImageVersions = ['2020_v10', '2021_v1', '2021_v2', '2021_v3.*']

    String googleTestVersion = "1.9.0-5-437e100-1"

    String jreArtifactLocation = "edu.wpi.first.jdk:roborio-2021:11.0.9u11-1"

    // WPILib (first.wpi.edu/FRC/roborio/maven) Utilities
    String smartDashboardVersion = "2021.2.2"
    String shuffleboardVersion = "2021.2.2"
    String outlineViewerVersion = "2021.2.2"
    String robotBuilderVersion = "2021.2.2"
    String robotBuilderOldVersion = "2021.2.2"
    String pathWeaverVersion = "2021.2.2"
    String glassVersion = "2021.2.2"

    WPIMavenExtension maven
    WPIDepsExtension deps

    String frcYear = '2021'

    NativePlatforms platforms;

    final Project project
    final String toolsClassifier
    final String cppToolsClassifier

    NativeUtilsExtension ntExt;

    @Inject
    WPIExtension(Project project) {
        this.project = project
        def factory = project.objects
        maven = factory.newInstance(WPIMavenExtension, project)

        if (project.hasProperty('forceToolsClassifier')) {
            this.toolsClassifier = project.findProperty('forceToolsClassifier')
        } else {
            this.toolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch") == 'amd64' ? 'win64' : 'win32' :
                            OperatingSystem.current().isMacOsX() ? "mac64" :
                                    OperatingSystem.current().isLinux() ? "linux64" :
                                            null
            )
        }
        if (project.hasProperty('forceCppToolsClassifier')) {
            this.cppToolsClassifier = project.findProperty('forceCppToolsClassifier')
        } else {
            this.cppToolsClassifier = (
                    OperatingSystem.current().isWindows() ?
                            System.getProperty("os.arch") == 'amd64' ? 'windowsx86-64' : 'windowsx86' :
                            OperatingSystem.current().isMacOsX() ? "osxx86-64" :
                                    OperatingSystem.current().isLinux() ? "linuxx86-64" :
                                            null
            )
        }

        platforms = new NativePlatforms();
        deps = factory.newInstance(WPIDepsExtension, project, this)
    }

    void maven(final Action<? super WPIMavenExtension> closure) {
      closure.execute(maven);
    }

    public void useLibrary(VariantComponentSpec component, String... libraries) {
        useRequiredLibrary(component, libraries)
    }

    public void useLibrary(NativeBinarySpec binary, String... libraries) {
        useRequiredLibrary(binary, libraries)
    }

    public void useRequiredLibrary(VariantComponentSpec component, String... libraries) {
        if (ntExt == null) {
            ntExt = project.extensions.getByType(NativeUtilsExtension)
        }
        ntExt.useRequiredLibrary(component, libraries)
    }

    public void useRequiredLibrary(NativeBinarySpec binary, String... libraries) {
        if (ntExt == null) {
            ntExt = project.extensions.getByType(NativeUtilsExtension)
        }
        ntExt.useRequiredLibrary(binary, libraries)
    }

    public void useOptionalLibrary(VariantComponentSpec component, String... libraries) {
        if (ntExt == null) {
            ntExt = project.extensions.getByType(NativeUtilsExtension)
        }
        ntExt.useOptionalLibrary(component, libraries)
    }

    public void useOptionalLibrary(NativeBinarySpec binary, String... libraries) {
        if (ntExt == null) {
            ntExt = project.extensions.getByType(NativeUtilsExtension)
        }
        ntExt.useOptionalLibrary(binary, libraries)
    }

    private String frcHomeCache

    String getFrcHome() {
        if (frcHomeCache != null) {
            return this.frcHomeCache
        }
        String frcHome = ''
        if (OperatingSystem.current().isWindows()) {
            String publicFolder = System.getenv('PUBLIC')
            if (publicFolder == null) {
                publicFolder = "C:\\Users\\Public"
            }
            def homeRoot = new File(publicFolder, "wpilib")
            frcHome = new File(homeRoot, this.frcYear).toString()
        } else {
            def userFolder = System.getProperty("user.home")
            def homeRoot = new File(userFolder, "wpilib")
            frcHome = new File(homeRoot, this.frcYear).toString()
        }
        frcHomeCache = frcHome
        return frcHomeCache
    }

    Map<String, Tuple> versions() {
        // Format:
        // property: [ PrettyName, Version, RecommendedKey ]
        return [
                "wpilibVersion"        : new Tuple("WPILib", wpilibVersion, "wpilib"),
                "opencvVersion"        : new Tuple("OpenCV", opencvVersion, "opencv"),
                "frcYear   "           : new Tuple("FRC Year", frcYear, "frcYear"),
                "googleTestVersion"    : new Tuple("Google Test", googleTestVersion, "googleTest"),
                "imguiVersion"         : new Tuple("ImGUI", imguiVersion, "imgui"),
                "wpimathVersion"       : new Tuple("WPIMath", wpimathVersion, "wpimath"),
                "ejmlVersion"          : new Tuple("EJML", ejmlVersion, "ejml"),
                "jacksonVersion"       : new Tuple("Jackson", jacksonVersion, "jackson"),

                "smartDashboardVersion": new Tuple("SmartDashboard", smartDashboardVersion, "smartdashboard"),
                "shuffleboardVersion"  : new Tuple("Shuffleboard", shuffleboardVersion, "shuffleboard"),
                "outlineViewerVersion" : new Tuple("OutlineViewer", outlineViewerVersion, "outlineviewer"),
                "robotBuilderVersion"  : new Tuple("RobotBuilder", robotBuilderVersion, "robotbuilder"),
                "RobotBuilderOldVersion"  : new Tuple("RobotBuilder-Old", robotBuilderOldVersion, "robotbuilderold"),
                "glassVersion"         : new Tuple("Glass", glassVersion, "glass"),
                "pathWeaverVersion"    : new Tuple("PathWeaver", pathWeaverVersion, "pathweaver"),
        ]
    }
}
