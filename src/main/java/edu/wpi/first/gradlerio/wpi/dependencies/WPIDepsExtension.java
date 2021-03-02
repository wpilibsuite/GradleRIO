package edu.wpi.first.gradlerio.wpi.dependencies;

import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;

public class WPIDepsExtension {

    private final WPIVersionsExtension wpi;

    private final WPIVendorDepsExtension vendor;

    public WPIVendorDepsExtension getVendor() {
        return vendor;
    }

    @Inject
    public WPIDepsExtension(Project project, WPIExtension wpi) {
        this.wpi = wpi.getVersions();
        this.vendor = project.getObjects().newInstance(WPIVendorDepsExtension.class, wpi);
    }

    // void wpilib(VariantComponentSpec component) {
    //     wpi.useLibrary(component, "wpilib_executable_shared", "vision_shared");
    // }

    // void wpilib(NativeBinarySpec binary) {
    //     wpi.useLibrary(binary, "wpilib_executable_shared", "vision_shared");
    // }

    // void wpilibStatic(VariantComponentSpec component) {
    //     wpi.useLibrary(component, "wpilib_executable_static", "vision_static");
    // }

    // void wpilibStatic(NativeBinarySpec binary) {
    //     wpi.useLibrary(binary, "wpilib_executable_static", "vision_static");
    // }

    // void googleTest(VariantComponentSpec component) {
    //     wpi.useLibrary(component, "googletest_static");
    // }

    // void googleTest(NativeBinarySpec binary) {
    //     wpi.useLibrary(binary, "googletest_static");
    // }

    // TODO make all of this lazy
    List<String> wpilib() {
        return List.of("edu.wpi.first.wpilibj:wpilibj-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.wpimath:wpimath-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.ntcore:ntcore-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.wpiutil:wpiutil-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.thirdparty.frc2021.opencv:opencv-java:" + wpi.getOpencvVersion().get(),
                "edu.wpi.first.cscore:cscore-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.cameraserver:cameraserver-java:" + wpi.getWpilibVersion().get(),
                "edu.wpi.first.hal:hal-java:" + wpi.getWpilibVersion().get(),
                "org.ejml:ejml-simple:" + wpi.getEjmlVersion().get(),
                "com.fasterxml.jackson.core:jackson-annotations:" + wpi.getJacksonVersion().get(),
                "com.fasterxml.jackson.core:jackson-core:" + wpi.getJacksonVersion().get(),
                "com.fasterxml.jackson.core:jackson-databind:" + wpi.getJacksonVersion().get());
    }

    List<String> wpilibSource() {
        return List.of("edu.wpi.first.wpilibj:wpilibj-java:" + wpi.getWpilibVersion().get() + ":sources",
                "edu.wpi.first.wpimath:wpimath-java:" + wpi.getWpilibVersion() + ":sources",
                "edu.wpi.first.ntcore:ntcore-java:" + wpi.getWpilibVersion() + ":sources",
                "edu.wpi.first.wpiutil:wpiutil-java:" + wpi.getWpilibVersion() + ":sources",
                "edu.wpi.first.thirdparty.frc2021.opencv:opencv-java:" + wpi.getOpencvVersion() + ":sources",
                "edu.wpi.first.cscore:cscore-java:" + wpi.getWpilibVersion() + ":sources",
                "edu.wpi.first.cameraserver:cameraserver-java:" + wpi.getWpilibVersion() + ":sources",
                "edu.wpi.first.hal:hal-java:" + wpi.getWpilibVersion() + ":sources",
                "org.ejml:ejml-simple:${wpi.ejmlVersion}:sources",
                "com.fasterxml.jackson.core:jackson-annotations:${wpi.jacksonVersion}:sources",
                "com.fasterxml.jackson.core:jackson-core:${wpi.jacksonVersion}:sources",
                "com.fasterxml.jackson.core:jackson-databind:${wpi.jacksonVersion}:sources");
    }

    List<String> wpilibJni(String platform) {
        return wpilibJniInternal(false, platform);
    }

    List<String> wpilibJniDebug(String platform) {
        return wpilibJniInternal(true, platform);
    }

    List<String> wpilibJniInternal(boolean debug, String platform) {
        String debugString = debug ? "debug" : "";

        // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
        // libraries, while the -jni ones are standalone (have static libs embedded).
        return List.of("edu.wpi.first.thirdparty.frc2021.opencv:opencv-cpp:" + wpi.getOpencvVersion().get() + ":" + platform + debugString + "@zip",
                "edu.wpi.first.hal:hal-cpp:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@zip",
                "edu.wpi.first.wpimath:wpimath-cpp:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@zip",
                "edu.wpi.first.wpiutil:wpiutil-cpp:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@zip",
                "edu.wpi.first.ntcore:ntcore-cpp:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@zip",
                "edu.wpi.first.cscore:cscore-cpp:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@zip");
    }

    List<String> wpilibEmbeddedJni(String platform) {
        return wpilibEmbeddedJniInternal(false, platform);
    }

    List<String> wpilibEmbeddedJniDebug(String platform) {
        return wpilibEmbeddedJniInternal(true, platform);
    }

    List<String> wpilibEmbeddedJniInternal(boolean debug, String platform) {
        String debugString = debug ? "debug" : "";

        // The JNI jars are for embedding into an output jar
        return List.of("edu.wpi.first.thirdparty.frc2021.opencv:opencv-jni:" + wpi.getOpencvVersion().get() + ":" + platform + debugString + "@jar",
                "edu.wpi.first.hal:hal-jni:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@jar",
                "edu.wpi.first.wpimath:wpimath-jni:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@jar",
                "edu.wpi.first.wpiutil:wpiutil-jni:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@jar",
                "edu.wpi.first.ntcore:ntcore-jni:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@jar",
                "edu.wpi.first.cscore:cscore-jni:" + wpi.getWpilibVersion().get() + ":" + platform + debugString + "@jar");
    }

}
