package edu.wpi.first.gradlerio.wpi.dependencies;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.nativeplatform.NativeBinarySpec;

import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.nativeutils.dependencies.ResolvedNativeDependency;
import edu.wpi.first.nativeutils.dependencies.WPIMavenDependency;
import edu.wpi.first.nativeutils.dependencies.WPISharedMavenDependency;
import edu.wpi.first.nativeutils.dependencies.WPIStaticMavenDependency;

public abstract class WPIVendorMavenDependency extends WPIMavenDependency {

    private WPIVendorDepsExtension.CppArtifact artifact;

    @Inject
    public WPIVendorMavenDependency(String name, Project project) {
        super(name, project);
    }

    private final Map<NativeBinarySpec, ResolvedNativeDependency> resolvedDependencies = new HashMap<>();

    @Override
    public ResolvedNativeDependency resolveNativeDependency(NativeBinarySpec binary) {
        ResolvedNativeDependency resolvedDep = resolvedDependencies.get(binary);
        if (resolvedDep != null) {
            return resolvedDep;
        }

        if (artifact.version.equals("wpilib")) {
            WPIExtension wpi = getProject().getExtensions().getByType(WPIExtension.class);
            getVersion().set(wpi.getWpilibVersion());
        } else {
            getVersion().set(artifact.version);
        }

        getGroupId().set(artifact.groupId);
        getArtifactId().set(artifact.artifactId);
        getExt().set("zip");
        getHeaderClassifier().set(artifact.headerClassifier);
        getSourceClassifier().set(artifact.sourcesClassifier);

        getTargetPlatforms().addAll(artifact.binaryPlatforms);

        FileCollection headers = getArtifactRoots(getHeaderClassifier().getOrElse(null));
        FileCollection sources = getArtifactRoots(getSourceClassifier().getOrElse(null));

        Set<String> targetPlatforms = getTargetPlatforms().get();
        String platformName = binary.getTargetPlatform().getName();
        if (!targetPlatforms.contains(platformName)) {
            if (artifact.skipInvalidPlatforms) {
                // return empty
                ResolvedNativeDependency dep = new ResolvedNativeDependency(headers, sources, getProject().files(),
                        getProject().files());
                resolvedDependencies.put(binary, dep);
                return dep;
            } else {
                return null;
            }
        }

        String buildType = binary.getBuildType().getName();

        FileCollection linkFiles;
        FileCollection runtimeFiles;

        if (artifact.sharedLibrary) {
            linkFiles = getArtifactFiles(platformName, buildType, WPISharedMavenDependency.SHARED_MATCHERS, WPISharedMavenDependency.SHARED_EXCLUDES);
            runtimeFiles = getArtifactFiles(platformName, buildType, WPISharedMavenDependency.RUNTIME_MATCHERS, WPISharedMavenDependency.RUNTIME_EXCLUDES);
        } else {
            linkFiles = getArtifactFiles(platformName + "static", buildType, WPIStaticMavenDependency.STATIC_MATCHERS, WPIStaticMavenDependency.EMPTY_LIST);
            runtimeFiles = getProject().files();
        }

        resolvedDep = new ResolvedNativeDependency(headers, sources, linkFiles, runtimeFiles);

        resolvedDependencies.put(binary, resolvedDep);
        return resolvedDep;
    }

    public void setArtifact(WPIVendorDepsExtension.CppArtifact artifact) {
        this.artifact = artifact;
    }

    public WPIVendorDepsExtension.CppArtifact getArtifact() {
        return artifact;
    }

}
