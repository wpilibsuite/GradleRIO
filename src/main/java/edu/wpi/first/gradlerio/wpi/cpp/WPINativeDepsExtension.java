package edu.wpi.first.gradlerio.wpi.cpp;

import javax.inject.Inject;

import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.nativeutils.NativeUtilsExtension;

public class WPINativeDepsExtension {
    private final NativeUtilsExtension nte;

    @Inject
    public WPINativeDepsExtension(NativeUtilsExtension nte) {
        this.nte = nte;
    }

    public void useLibrary(VariantComponentSpec component, String... libraries) {
        useRequiredLibrary(component, libraries);
    }

    public void useLibrary(NativeBinarySpec binary, String... libraries) {
        useRequiredLibrary(binary, libraries);
    }

    public void useRequiredLibrary(VariantComponentSpec component, String... libraries) {
        nte.useRequiredLibrary(component, libraries);
    }

    public void useRequiredLibrary(NativeBinarySpec binary, String... libraries) {
        nte.useRequiredLibrary(binary, libraries);
    }

    public void useOptionalLibrary(VariantComponentSpec component, String... libraries) {
        nte.useOptionalLibrary(component, libraries);
    }

    public void useOptionalLibrary(NativeBinarySpec binary, String... libraries) {
        nte.useOptionalLibrary(binary, libraries);
    }

    void wpilib(VariantComponentSpec component) {
        useLibrary(component, "wpilib_executable_shared", "vision_shared");
    }

    void wpilib(NativeBinarySpec binary) {
        useLibrary(binary, "wpilib_executable_shared", "vision_shared");
    }

    void wpilibStatic(VariantComponentSpec component) {
        useLibrary(component, "wpilib_executable_static", "vision_static");
    }

    void wpilibStatic(NativeBinarySpec binary) {
        useLibrary(binary, "wpilib_executable_static", "vision_static");
    }

    void googleTest(VariantComponentSpec component) {
        useLibrary(component, "googletest_static");
    }

    void googleTest(NativeBinarySpec binary) {
        useLibrary(binary, "googletest_static");
    }
}
