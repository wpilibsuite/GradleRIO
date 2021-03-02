package edu.wpi.first.gradlerio.wpi.nativebuild;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
import edu.wpi.first.nativeutils.NativeUtils;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.ToolchainExtension;
import edu.wpi.first.toolchain.ToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;
import edu.wpi.first.vscode.GradleVsCode;

public class WPINativeExtension {
    private final WPINativeDepsExtension deps;
    private final WPINativeVendorDepsExtension vendor;

    public WPINativeDepsExtension getDeps() {
        return deps;
    }

    public WPINativeVendorDepsExtension getVendor() {
        return vendor;
    }

    @Inject
    public WPINativeExtension(Project project, WPIVendorDepsExtension vendorDeps, WPIVersionsExtension versions) {
        project.getPluginManager().apply(ToolchainPlugin.class);
        project.getPluginManager().apply(RoboRioToolchainPlugin.class);
        project.getPluginManager().apply(NativeUtils.class);
        project.getPluginManager().apply(WPINativeCompileRules.class);

        NativeUtilsExtension nte = project.getExtensions().getByType(NativeUtilsExtension.class);
        nte.withRoboRIO();
        nte.addWpiNativeUtils();

        deps = project.getObjects().newInstance(WPINativeDepsExtension.class, nte);

        vendor = project.getObjects().newInstance(WPINativeVendorDepsExtension.class, vendorDeps, nte, project);

        ToolchainExtension te = project.getExtensions().getByType(ToolchainExtension.class);
        te.getCrossCompilers().named(nte.getWpi().platforms.roborio, c -> {
            c.getOptional().set(false);
        });

        nte.getWpi().addWarnings();
        //nte.setSinglePrintPerPlatform();

        nte.getWpi().configureDependencies(wpiDeps -> {
            wpiDeps.getWpiVersion().set(versions.getWpilibVersion());
            wpiDeps.getNiLibVersion().set(versions.getNiLibrariesVersion());
            wpiDeps.getOpencvVersion().set(versions.getOpencvVersion());
            wpiDeps.getGoogleTestVersion().set(versions.getGoogleTestVersion());
            wpiDeps.getImguiVersion().set(versions.getImguiVersion());
            wpiDeps.getWpimathVersion().set(versions.getWpimathVersion());
        });

        project.getPluginManager().apply(GradleVsCode.class);
    }
}
