package edu.wpi.first.gradlerio.wpi.java;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;

public class WPIJavaDepsExtension {

    private final ProviderFactory providers;

    private static String dependencyNotation(String groupId, String artifactId, Provider<String> version) {
        return groupId + ":" + artifactId + ":" + version.get();
    }

    private void createJavaDependencies(String groupdId, String artifactId, Provider<String> version) {
        wpilibDeps.add(providers.provider(() -> {
            return dependencyNotation(groupdId, artifactId, version);
        }));

        wpilibSources.add(providers.provider(() -> {
            return dependencyNotation(groupdId, artifactId, version) + ":sources";
        }));
    }

    private Provider<String> createJniDependency(String groupdId, String artifactId, Provider<String> version, boolean debug, String platform) {
        String debugString = debug ? "debug" : "";
        return providers.provider(() -> {
            return dependencyNotation(groupdId, artifactId, version) + ":" + platform + debugString + "@zip";
        });
    }

    private final List<Provider<String>> wpilibDeps = new ArrayList<>();
    private final List<Provider<String>> wpilibSources = new ArrayList<>();
    private final WPIVersionsExtension versions;

    @Inject
    public WPIJavaDepsExtension(WPIVersionsExtension versions, ProviderFactory providers) {
        this.providers = providers;
        this.versions = versions;

        createJavaDependencies("edu.wpi.first.wpilibj", "wpilibj-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.wpimath", "wpimath-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.ntcore", "ntcore-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.cscore", "cscore-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.cameraserver", "cameraserver-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.hal", "hal-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.wpinet", "wpinet-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.wpiutil", "wpiutil-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.apriltag", "apriltag-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.wpiunits", "wpiunits-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.epilogue", "epilogue-runtime-java", versions.getWpilibVersion());
        createJavaDependencies("edu.wpi.first.datalog", "datalog-java", versions.getWpilibVersion());

        createJavaDependencies("org.wpilib", "annotations-java", versions.getWpilibVersion());

        createJavaDependencies("edu.wpi.first.thirdparty.frc2025.opencv", "opencv-java", versions.getOpencvVersion());
        createJavaDependencies("org.ejml", "ejml-simple", versions.getEjmlVersion());

        createJavaDependencies("com.fasterxml.jackson.core", "jackson-annotations", versions.getJacksonVersion());
        createJavaDependencies("com.fasterxml.jackson.core", "jackson-core", versions.getJacksonVersion());
        createJavaDependencies("com.fasterxml.jackson.core", "jackson-databind", versions.getJacksonVersion());

        createJavaDependencies("us.hebi.quickbuf", "quickbuf-runtime", versions.getQuickbufVersion());
    }

    public List<Provider<String>> wpilib() {
        return wpilibDeps;
    }

    /** Dependencies required for using WPILib's Java annotations during compilation. */
    public List<Provider<String>> wpilibAnnotations() {
        // epilogue-runtime is a dependency of epilogue-processor, and needs to be on the annotation processor
        // classpath at compile time for the processor to function. Same with annotations for wpilibj-javac-plugin.
        return List.of(
                providers.provider(() -> dependencyNotation("edu.wpi.first.epilogue", "epilogue-processor-java", versions.getWpilibVersion())),
                providers.provider(() -> dependencyNotation("edu.wpi.first.epilogue", "epilogue-runtime-java", versions.getWpilibVersion())),
                providers.provider(() -> dependencyNotation("org.wpilib", "wpilibj-javac-plugin-java", versions.getWpilibVersion())),
                providers.provider(() -> dependencyNotation("org.wpilib", "annotations-java", versions.getWpilibVersion()))
        );
    }

    public List<Provider<String>> wpilibSources() {
        return wpilibSources;
    }

    public List<Provider<String>> wpilibJniDebug(String platform) {
        return getWpilibJniInternal(true, platform);
    }

    public List<Provider<String>> wpilibJniRelease(String platform) {
        return getWpilibJniInternal(false, platform);
    }

    private List<Provider<String>> getWpilibJniInternal(boolean debug, String platform) {
        return List.of(
            createJniDependency("edu.wpi.first.hal", "hal-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.wpimath", "wpimath-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.ntcore", "ntcore-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.cscore", "cscore-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.thirdparty.frc2025.opencv", "opencv-cpp", versions.getOpencvVersion(), debug, platform),
            createJniDependency("edu.wpi.first.wpinet", "wpinet-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.wpiutil", "wpiutil-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.apriltag", "apriltag-cpp", versions.getWpilibVersion(), debug, platform),
            createJniDependency("edu.wpi.first.datalog", "datalog-cpp", versions.getWpilibVersion(), debug, platform)
        );
    }
}
