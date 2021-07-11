package edu.wpi.first.gradlerio.wpi.cpp;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension.CppArtifact;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension.JsonDependency;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.nativeutils.dependencies.AllPlatformsCombinedNativeDependency;

public class WPINativeVendorDepsExtension {
    private final WPIVendorDepsExtension vendorDeps;
    private final NativeUtilsExtension nte;

    @Inject
    public WPINativeVendorDepsExtension(WPIVendorDepsExtension vendorDeps, NativeUtilsExtension nte, Project project) {
        this.nte = nte;
        this.vendorDeps = vendorDeps;

        initializeNativeDependencies(project);
    }

    public void initializeNativeDependencies(Project project) {
        var dependencyContainer = nte.getNativeDependencyContainer();
        dependencyContainer.registerFactory(WPIVendorMavenDependency.class, name -> {
            return project.getObjects().newInstance(WPIVendorMavenDependency.class, name, project);
        });

        for (JsonDependency dep : vendorDeps.getDependenciesMap().values()) {
            // Individual dependencies
            if (dep.cppDependencies.length <= 0) {
                continue;
            }

            String depName = dep.uuid + "_" + dep.name;

            AllPlatformsCombinedNativeDependency combinedDep = dependencyContainer.create(depName, AllPlatformsCombinedNativeDependency.class);

            for (CppArtifact cpp : dep.cppDependencies) {
                String name = depName + "_" + cpp.libName;
                combinedDep.getDependencies().add(name);
                WPIVendorMavenDependency vendorDep = dependencyContainer.create(name, WPIVendorMavenDependency.class);
                vendorDep.setArtifact(cpp);
            }
        }
    }

    public void cpp(Object scope, String... ignore) {
        if (scope instanceof VariantComponentSpec) {
            ((VariantComponentSpec) scope).getBinaries().withType(NativeBinarySpec.class).all(bin -> {
                cppVendorLibForBin(bin, ignore);
            });
        } else if (scope instanceof NativeBinarySpec) {
            cppVendorLibForBin((NativeBinarySpec) scope, ignore);
        } else {
            throw new GradleException(
                    "Unknown type for useVendorLibraries target. You put this declaration in a weird place.");
        }
    }

    private void cppVendorLibForBin(NativeBinarySpec bin, String[] ignore) {

        for (JsonDependency dep : vendorDeps.getDependenciesMap().values()) {
            if (WPIVendorDepsExtension.isIgnored(ignore, dep)) {
                continue;
            }
            nte.useRequiredLibrary(bin, dep.uuid + "_" + dep.name);
        }
    }
}
