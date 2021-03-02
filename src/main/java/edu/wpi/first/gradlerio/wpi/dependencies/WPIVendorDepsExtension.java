package edu.wpi.first.gradlerio.wpi.dependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.gson.Gson;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
// import edu.wpi.first.deployutils.nativedeps.DelegatedDependencySet;
// import edu.wpi.first.deployutils.nativedeps.DependencySpecExtension;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.nativeutils.dependencies.AllPlatformsCombinedNativeDependency;
//import edu.wpi.first.nativeutils.configs.DependencyConfig;
import edu.wpi.first.nativeutils.dependencies.NativeDependency;

public abstract class WPIVendorDepsExtension {

    private final WPIExtension wpiExt;

    private final Map<String, JsonDependency> dependencies = new HashMap<>();

    public List<JsonDependency> getDependencies() {
        return new ArrayList<>(dependencies.values());
    }

    // private final List<DelegatedDependencySet> nativeDependenciesList = new
    // ArrayList<>();

    // public List<DelegatedDependencySet> getNativeDependenciesList() {
    // return nativeDependenciesList;
    // }

    private final ETLogger log;
    private final Gson gson = new Gson();

    public static final String DEFAULT_VENDORDEPS_FOLDER_NAME = "vendordeps";
    public static final String GRADLERIO_VENDOR_FOLDER_PROPERTY = "gradlerio.vendordep.folder.path";

    private final ProviderFactory providerFactory;

    @Inject
    public WPIVendorDepsExtension(WPIExtension wpiExt, ProviderFactory providerFactory) {
        this.wpiExt = wpiExt;
        this.log = ETLoggerFactory.INSTANCE.create("WPIVendorDeps");
        this.providerFactory = providerFactory;
    }

    private File vendorFolder(Project project) {
        Object prop = project.findProperty(GRADLERIO_VENDOR_FOLDER_PROPERTY);
        String filepath = DEFAULT_VENDORDEPS_FOLDER_NAME;
        if (prop != null && !prop.equals(DEFAULT_VENDORDEPS_FOLDER_NAME)) {
            // TODO Fix these
            log.logErrorHead(
                    "Warning! You have the property $GRADLERIO_VENDOR_FOLDER_PROPERTY set to a non-default value: $prop");
            log.logError("The default path (from the project root) is $DEFAULT_VENDORDEPS_FOLDER_NAME");
            log.logError(
                    "This can cause GradleRIO to not be able to find the vendordep JSON files, and the dependencies not being loaded.");
            log.logError("This can result in compilation errors and you not being able to deploy code.");
            log.logError("Remove this from your gradle.properties file unless you know what you're doing.");
            filepath = (String) prop;
        }
        return project.file(filepath);
    }

    public static List<File> vendorFiles(File directory) {
        if (directory.exists()) {
            return List.of(directory.listFiles(pathname -> {
                return pathname.getName().endsWith(".json");
            }));
        } else {
            return List.of();
        }
    }

    public void loadAll() {
        loadFrom(vendorFolder(wpiExt.getProject()));
    }

    public void loadFrom(File directory) {
        for (File f : vendorFiles(directory)) {
            JsonDependency dep = parse(f);
            if (dep != null) {
                load(dep);
            }
        }
    }

    public void loadFrom(Project project) {
        loadFrom(vendorFolder(project));
    }

    private JsonDependency parse(File f) {
        try (BufferedReader reader = Files.newBufferedReader(f.toPath())) {
            return gson.fromJson(reader, JsonDependency.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void load(JsonDependency dep) {
        // Don"t double-add a dependency!
        if (dependencies.putIfAbsent(dep.uuid, dep) != null) {
            return;
        }

        if (dep != null && dep.mavenUrls != null) {
            int i = 0;
            for (String url : dep.mavenUrls) {
                // Only add if the maven doesn"t yet exist.

                if (wpiExt.getMaven().matching(x -> x.getRelease().equals(url)).isEmpty()) {
                    String name = dep.uuid + "_" + i++;
                    log.info("Registering vendor dep maven: " + name + " on project " + wpiExt.getProject().getPath());
                    wpiExt.getMaven().vendor(name, repo -> repo.setRelease(url));
                }
            }
        }
    }



    public static String getVersion(String inputVersion, ProviderFactory providers, WPIVersionsExtension wpiExt) {
        return inputVersion.equals("wpilib") ? wpiExt.getWpilibVersion().get() : inputVersion;
    }

    public List<Provider<String>> java(String... ignore) {
        return dependencies.entrySet().stream().map(x -> x.getValue()).filter(x -> !isIgnored(ignore, x))
                .map(x -> List.of(x.javaDependencies)).flatMap(List<JavaArtifact>::stream).map(art -> {
                    Callable<String> cbl = () -> art.groupId + ":" + art.artifactId + ":" + getVersion(art.version, providerFactory, wpiExt.getVersions());
                    return providerFactory.provider(cbl);
                }).collect(Collectors.toList());
    }

    public List<Provider<String>> jni(String platform, String... ignore) {
        return jniInternal(false, platform, ignore);
    }

    public List<Provider<String>> jniDebug(String platform, String... ignore) {
        return jniInternal(true, platform, ignore);
    }

    private List<Provider<String>> jniInternal(boolean debug, String platform, String... ignore) {

        List<Provider<String>> deps = new ArrayList<>();

        for (JsonDependency dep : dependencies.values()) {
            if (!isIgnored(ignore, dep)) {
                for (JniArtifact jni : dep.jniDependencies) {
                    boolean applies = Arrays.asList(jni.validPlatforms).contains(platform);
                    if (!applies && !jni.skipInvalidPlatforms)
                        throw new WPIDependenciesPlugin.MissingJniDependencyException(dep.name, platform, jni);

                    if (applies) {
                        String debugString = debug ? "debug" : "";
                        Callable<String> cbl = () -> jni.groupId + ":" + jni.artifactId + ":" + getVersion(jni.version, providerFactory, wpiExt.getVersions()) + ":"
                                + platform + debugString + "@" + (jni.isJar ? "jar" : "zip");
                        deps.add(providerFactory.provider(cbl));
                    }
                }
            }
        }
        return deps;
    }

    public void initializeNativeDependencies(NativeUtilsExtension nte, Project project) {
        var dependencyContainer = nte.getNativeDependencyContainer();
        dependencyContainer.registerFactory(WPIVendorMavenDependency.class, name -> {
            return project.getObjects().newInstance(WPIVendorMavenDependency.class, name, project);
        });

        for (JsonDependency dep : dependencies.values()) {
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
        // NativeUtilsExtension nue =
        // wpiExt.getProject().getExtensions().getByType(NativeUtilsExtension.class);
        // for (JsonDependency dep : dependencies.values()) {
        // // Individual dependencies
        // if (dep.cppDependencies.length <= 0) {
        // continue;
        // }

        // String depName = dep.uuid + "_" + dep.name;

        // NamedDomainObjectContainer<DependencyConfig> dConfigs =
        // nue.getDependencyConfigs();

        // for (CppArtifact cpp : dep.cppDependencies) {
        // String name = depName + "_" + cpp.libName;
        // dConfigs.create(name, c -> {
        // c.setGroupId(cpp.groupId);
        // c.setArtifactId(cpp.artifactId);
        // c.setHeaderClassifier(cpp.headerClassifier);
        // c.setSourceClassifier(cpp.sourcesClassifier);
        // c.setVersion(cpp.version);
        // c.setExt("zip");
        // if (cpp.sharedLibrary) {
        // c.getSharedPlatforms().addAll(Arrays.asList(cpp.binaryPlatforms));
        // } else {
        // c.getStaticPlatforms().addAll(Arrays.asList(cpp.binaryPlatforms));
        // }
        // c.setSkipMissingPlatform(cpp.skipInvalidPlatforms);
        // c.setSkipCombinedDependency(true);
        // });
        // }

        // nue.getCombinedDependencyConfigs().create(depName, combined -> {
        // for (CppArtifact cpp : dep.cppDependencies) {
        // String name = depName + "_" + cpp.libName;
        // String binaryName = name;
        // if (cpp.sharedLibrary) {
        // binaryName = name + "_shared_binaries";
        // } else {
        // binaryName = name + "_static_binaries";
        // }
        // combined.getDependencies().add(binaryName);
        // if (cpp.headerClassifier != null) {
        // combined.getDependencies().add(name + "_headers");
        // }

        // if (cpp.sourcesClassifier != null) {
        // combined.getDependencies().add(name + "_sources");
        // }
        // }
        // });
        // }
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
        NativeUtilsExtension nue = wpiExt.getProject().getExtensions().getByType(NativeUtilsExtension.class);

        for (JsonDependency dep : dependencies.values()) {
            if (isIgnored(ignore, dep)) {
                continue;
            }
            nue.useRequiredLibrary(bin, dep.uuid + "_" + dep.name);
        }
    }

    private boolean isIgnored(String[] ignore, JsonDependency dep) {
        for (String i : ignore) {
            if (i.equals(dep.name) || i.equals(dep.uuid)) {
                return true;
            }
        }
        return false;
    }

    public static class JavaArtifact {
        String groupId;
        String artifactId;
        String version;
    }

    public static class JniArtifact {
        String groupId;
        String artifactId;
        String version;

        boolean isJar;

        String[] validPlatforms;
        boolean skipInvalidPlatforms;
    }

    public static class CppArtifact {
        String groupId;
        String artifactId;
        String version;
        String libName;

        String headerClassifier;
        String sourcesClassifier;
        String[] binaryPlatforms;
        boolean skipInvalidPlatforms;

        boolean sharedLibrary;
    }

    public static class JsonDependency {
        String name;
        String version;
        String uuid;
        String[] mavenUrls;
        String jsonUrl;
        String fileName;
        JavaArtifact[] javaDependencies;
        JniArtifact[] jniDependencies;
        CppArtifact[] cppDependencies;
    }

}
