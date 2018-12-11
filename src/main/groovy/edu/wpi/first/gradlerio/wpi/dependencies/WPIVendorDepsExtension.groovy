package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.DelegatedDependencySet
import jaci.gradle.nativedeps.DependencySpecExtension
import org.gradle.api.GradleException
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.VariantComponentSpec

@CompileStatic
public class WPIVendorDepsExtension {

    final WPIDepsExtension wpiDeps
    final WPIExtension wpiExt

    List<JsonDependency> dependencies = []
    final List<DelegatedDependencySet> nativeDependenciesList = []

    final File vendorFolder;

    WPIVendorDepsExtension(WPIDepsExtension wpiDeps, WPIExtension wpiExt) {
        this.wpiDeps = wpiDeps
        this.wpiExt = wpiExt
        this.vendorFolder = wpiDeps.wpi.project.file('vendordeps')
    }

    @CompileDynamic
    void loadDependency(Object slurped) {
        JsonDependency dep = new JsonDependency(slurped)
        dependencies.add(dep)
    }

    List<File> allVendorFiles() {
        if (vendorFolder.exists()) {
            return vendorFolder.listFiles(new FileFilter() {
                @Override
                boolean accept(File pathname) {
                    return pathname.name.endsWith(".json")
                }
            }) as List<File>
        } else
            return []
    }

    static String getVersion(String inputVersion, WPIExtension wpiExt) {
        return inputVersion == 'wpilib' ? wpiExt.wpilibVersion : inputVersion
    }

    List<String> java(String... ignore) {
        if (dependencies == null) return []

        return dependencies.findAll { !isIgnored(ignore, it) }.collectMany { JsonDependency dep ->
            dep.javaDependencies.collect { JavaArtifact art ->
                "${art.groupId}:${art.artifactId}:${getVersion(art.version, wpiExt)}"
            } as List<String>
        }
    }

    List<String> jni(String platform, String... ignore) {
        if (dependencies == null) return []

        def deps = [] as List<String>

        dependencies.each { JsonDependency dep ->
            if (!isIgnored(ignore, dep)) {
                dep.jniDependencies.each { JniArtifact jni ->
                    boolean applies = jni.validPlatforms.contains(platform)
                    if (!applies && !jni.skipInvalidPlatforms)
                        throw new WPIDependenciesPlugin.MissingJniDependencyException(dep.name, platform, jni)

                    if (applies)
                        deps.add("${jni.groupId}:${jni.artifactId}:${getVersion(jni.version, wpiExt)}:${platform}@${jni.isJar ? 'jar' : 'zip'}".toString())
                }
            }
        }

        return deps
    }

    void cpp(Object scope, String... ignore) {
        def dse = wpiDeps.wpi.project.extensions.getByType(DependencySpecExtension)
        if (scope in VariantComponentSpec) {
            ((VariantComponentSpec)scope).binaries.withType(NativeBinarySpec).all { NativeBinarySpec bin ->
                cppVendorLibForBin(dse, bin, ignore)
            }
        } else if (scope in NativeBinarySpec) {
            cppVendorLibForBin(dse, (NativeBinarySpec)scope, ignore)
        } else {
            throw new GradleException('Unknown type for useVendorLibraries target. You put this declaration in a weird place.')
        }
    }

    private void cppVendorLibForBin(DependencySpecExtension dse, NativeBinarySpec bin, String[] ignore) {
        Set<DelegatedDependencySet> dds = []
        String buildType = bin.buildType.name.contains('debug') ? 'debug' : ''
        dependencies.each { JsonDependency dep ->
            if (!isIgnored(ignore, dep)) {
                dep.cppDependencies.each { CppArtifact cpp ->
                    if (cpp.headerClassifier != null)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_headers" + buildType, bin, dse, cpp.skipInvalidPlatforms))
                    if (cpp.sourcesClassifier != null)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_sources" + buildType, bin, dse, cpp.skipInvalidPlatforms))
                    if (cpp.binaryPlatforms != null && cpp.binaryPlatforms.length > 0)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_binaries" + buildType, bin, dse, cpp.skipInvalidPlatforms))
                }
            }
        }

        dds.each { DelegatedDependencySet set ->
            bin.lib(set)
        }
    }

    private boolean isIgnored(String[] ignore, JsonDependency dep) {
        return ignore.find { it.equals(dep.name) || it.equals(dep.uuid) } != null
    }

    static class JavaArtifact {
        String groupId
        String artifactId
        String version
    }

    static class JniArtifact {
        String groupId
        String artifactId
        String version

        boolean isJar

        String[] validPlatforms
        boolean skipInvalidPlatforms
    }

    static class CppArtifact {
        String groupId
        String artifactId
        String version
        String libName
        String configuration

        String headerClassifier
        String sourcesClassifier
        String[] binaryPlatforms
        boolean skipInvalidPlatforms

        boolean sharedLibrary
    }

    static class JsonDependency {
        String name
        String version
        String uuid
        String[] mavenUrls
        String jsonUrl
        String fileName
        JavaArtifact[] javaDependencies
        JniArtifact[] jniDependencies
        CppArtifact[] cppDependencies
    }

}
