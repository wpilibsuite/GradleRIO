package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.wpi.dependencies.WPIDepsExtension;
import edu.wpi.first.nativeutils.NativeUtils;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.ToolchainExtension;
import edu.wpi.first.toolchain.ToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;
import edu.wpi.first.vscode.GradleVsCode;

public class WPINativeExtension {
    private final NativeUtilsExtension nte;

    @Inject
    public WPINativeExtension(Project project, WPIDepsExtension depsExt, WPIVersionsExtension versions) {
        project.getPluginManager().apply(ToolchainPlugin.class);
        project.getPluginManager().apply(RoboRioToolchainPlugin.class);
        project.getPluginManager().apply(NativeUtils.class);
        project.getPluginManager().apply(WPINativeCompileRules.class);

        nte = project.getExtensions().getByType(NativeUtilsExtension.class);
        nte.withRoboRIO();
        nte.addWpiNativeUtils();

        depsExt.getVendor().initializeNativeDependencies(nte, project);

        ToolchainExtension te = project.getExtensions().getByType(ToolchainExtension.class);
        te.getCrossCompilers().named(nte.getWpi().platforms.roborio, c -> {
            c.getOptional().set(false);
        });

        nte.getWpi().addWarnings();
        //nte.setSinglePrintPerPlatform();

        nte.getWpi().configureDependencies(deps -> {
            deps.getWpiVersion().set(versions.getWpilibVersion());
            deps.getNiLibVersion().set(versions.getNiLibrariesVersion());
            deps.getOpencvVersion().set(versions.getOpencvVersion());
            deps.getGoogleTestVersion().set(versions.getGoogleTestVersion());
            deps.getImguiVersion().set(versions.getImguiVersion());
            deps.getWpimathVersion().set(versions.getWpimathVersion());
        });

        project.getPluginManager().apply(GradleVsCode.class);
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
}
