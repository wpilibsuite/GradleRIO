package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.VariantComponentSpec

import javax.inject.Inject

@CompileStatic
public class WPIDepsExtension {

    final WPIExtension wpi

    final WPIVendorDepsExtension vendor
    final WPISimDepsExtension sim

    @Inject
    WPIDepsExtension(Project project, WPIExtension wpi) {
        this.wpi = wpi
        this.vendor = project.objects.newInstance(WPIVendorDepsExtension, wpi)
        this.sim = project.objects.newInstance(WPISimDepsExtension, wpi)
    }

    void wpilib(VariantComponentSpec component) {
        wpi.useLibrary(component, 'wpilib_executable_shared', 'vision_shared')
    }

    void wpilib(NativeBinarySpec binary) {
        wpi.useLibrary(binary, 'wpilib_executable_shared', 'vision_shared')
    }

    void wpilibStatic(VariantComponentSpec component) {
        wpi.useLibrary(component, 'wpilib_executable_static', 'vision_static')
    }

    void wpilibStatic(NativeBinarySpec binary) {
        wpi.useLibrary(binary, 'wpilib_executable_static', 'vision_static')
    }

    void googleTest(VariantComponentSpec component) {
        wpi.useLibrary(component, 'googletest_static')
    }

    void googleTest(NativeBinarySpec binary) {
        wpi.useLibrary(binary, 'googletest_static')
    }

    List<String> wpilib() {
        return ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.wpimath:wpimath-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.thirdparty.frc2021.opencv:opencv-java:${wpi.opencvVersion}".toString(),
                "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}".toString(),
                "org.ejml:ejml-simple:${wpi.ejmlVersion}".toString(),
                "com.fasterxml.jackson.core:jackson-annotations:${wpi.jacksonVersion}".toString(),
                "com.fasterxml.jackson.core:jackson-core:${wpi.jacksonVersion}".toString(),
                "com.fasterxml.jackson.core:jackson-databind:${wpi.jacksonVersion}".toString()]
    }

    List<String> wpilibSource() {
        return ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.wpimath:wpimath-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.thirdparty.frc2021.opencv:opencv-java:${wpi.opencvVersion}:sources".toString(),
                "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}:sources".toString(),
                "org.ejml:ejml-simple:${wpi.ejmlVersion}:sources".toString(),
                "com.fasterxml.jackson.core:jackson-annotations:${wpi.jacksonVersion}:sources".toString(),
                "com.fasterxml.jackson.core:jackson-core:${wpi.jacksonVersion}:sources".toString(),
                "com.fasterxml.jackson.core:jackson-databind:${wpi.jacksonVersion}:sources".toString()]
    }

    List<String> wpilibJni(String platform) {
        return wpilibJniInternal(false, platform)
    }

    List<String> wpilibJniDebug(String platform) {
        return wpilibJniInternal(true, platform);
    }

    List<String> wpilibJniInternal(boolean debug, String platform) {
        def debugString = debug ? "debug" : ""

        // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
        // libraries, while the -jni ones are standalone (have static libs embedded).
        return ["edu.wpi.first.thirdparty.frc2021.opencv:opencv-cpp:${wpi.opencvVersion}:${platform}${debugString}@zip".toString(),
                "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${platform}${debugString}@zip".toString(),
                "edu.wpi.first.wpimath:wpimath-cpp:${wpi.wpilibVersion}:${platform}${debugString}@zip".toString(),
                "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${platform}${debugString}@zip".toString(),
                "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${platform}${debugString}@zip".toString(),
                "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${platform}${debugString}@zip".toString()]
    }

    List<String> wpilibEmbeddedJni(String platform) {
        return wpilibEmbeddedJniInternal(false, platform)
    }

    List<String> wpilibEmbeddedJniDebug(String platform) {
        return wpilibEmbeddedJniInternal(true, platform);
    }

    List<String> wpilibEmbeddedJniInternal(boolean debug, String platform) {
        def debugString = debug ? "debug" : ""

        // The JNI jars are for embedding into an output jar
        return ["edu.wpi.first.thirdparty.frc2021.opencv:opencv-jni:${wpi.opencvVersion}:${platform}${debugString}@jar".toString(),
                "edu.wpi.first.hal:hal-jni:${wpi.wpilibVersion}:${platform}${debugString}@jar".toString(),
                "edu.wpi.first.wpimath:wpimath-jni:${wpi.wpilibVersion}:${platform}${debugString}@jar".toString(),
                "edu.wpi.first.wpiutil:wpiutil-jni:${wpi.wpilibVersion}:${platform}${debugString}@jar".toString(),
                "edu.wpi.first.ntcore:ntcore-jni:${wpi.wpilibVersion}:${platform}${debugString}@jar".toString(),
                "edu.wpi.first.cscore:cscore-jni:${wpi.wpilibVersion}:${platform}${debugString}@jar".toString()]
    }

}
