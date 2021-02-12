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
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.gson.Gson;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.platform.base.VariantComponentSpec;

import edu.wpi.first.embeddedtools.log.ETLogger;
import edu.wpi.first.embeddedtools.log.ETLoggerFactory;
import edu.wpi.first.embeddedtools.nativedeps.DelegatedDependencySet;
import edu.wpi.first.embeddedtools.nativedeps.DependencySpecExtension;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class WPIVendorDepsExtension {

    private final WPIExtension wpiExt;

    public WPIExtension getWpiExt() {
        return wpiExt;
    }

    private final Map<String, JsonDependency> dependencies = new HashMap<>();

    public List<JsonDependency> getDependencies() {
        return new ArrayList<>(dependencies.values());
    }

    private final List<DelegatedDependencySet> nativeDependenciesList = new ArrayList<>();

    public List<DelegatedDependencySet> getNativeDependenciesList() {
        return nativeDependenciesList;
    }

    private final ETLogger log;
    private final Gson gson = new Gson();

    public static final String DEFAULT_VENDORDEPS_FOLDER_NAME = "vendordeps";
    public static final String GRADLERIO_VENDOR_FOLDER_PROPERTY = "gradlerio.vendordep.folder.path";

    @Inject
    public WPIVendorDepsExtension(WPIExtension wpiExt) {
        this.wpiExt = wpiExt;
        this.log = ETLoggerFactory.INSTANCE.create("WPIVendorDeps");
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

    public static String getVersion(String inputVersion, WPIExtension wpiExt) {
        return inputVersion.equals("wpilib") ? wpiExt.getWpilibVersion() : inputVersion;
    }

    public List<String> java(String... ignore) {
        return dependencies.entrySet().stream()
            .map(x -> x.getValue())
            .filter(x -> !isIgnored(ignore, x))
            .map(x -> List.of(x.javaDependencies))
            .flatMap(List<JavaArtifact>::stream)
            .map(art -> {
                return art.groupId + ":" + art.artifactId + ":" + getVersion(art.version, wpiExt);
            })
            .collect(Collectors.toList());
    }

    public List<String> jni(String platform, String... ignore) {
        return jniInternal(false, platform, ignore);
    }

    public List<String> jniDebug(String platform, String... ignore) {
        return jniInternal(true, platform, ignore);
    }

    private List<String> jniInternal(boolean debug, String platform, String... ignore) {

        List<String> deps = new ArrayList<>();

        for (JsonDependency dep : dependencies.values()) {
            if (!isIgnored(ignore, dep)) {
                for (JniArtifact jni : dep.jniDependencies) {
                    boolean applies = Arrays.asList(jni.validPlatforms).contains(platform);
                    if (!applies && !jni.skipInvalidPlatforms)
                        throw new WPIDependenciesPlugin.MissingJniDependencyException(dep.name, platform, jni);

                    if (applies) {
                        String debugString = debug ? "debug" : "";
                        deps.add(jni.groupId + ":" + jni.artifactId + ":" + getVersion(jni.version, wpiExt) + ":" + platform + debugString + "@" + (jni.isJar ? "jar" : "zip"));
                    }
                }
            }
        }
        return deps;
    }

    public void cpp(Object scope, String... ignore) {
        DependencySpecExtension dse = wpiExt.getProject().getExtensions().getByType(DependencySpecExtension.class);
        if (scope instanceof VariantComponentSpec) {
            ((VariantComponentSpec)scope).getBinaries().withType(NativeBinarySpec.class).all( bin -> {
                cppVendorLibForBin(dse, bin, ignore);
            });
        } else if (scope instanceof NativeBinarySpec) {
            cppVendorLibForBin(dse, (NativeBinarySpec)scope, ignore);
        } else {
            throw new GradleException("Unknown type for useVendorLibraries target. You put this declaration in a weird place.");
        }
    }

    private void cppVendorLibForBin(DependencySpecExtension dse, NativeBinarySpec bin, String[] ignore) {
        Set<DelegatedDependencySet> dds = new HashSet<>();
        for (JsonDependency dep : dependencies.values()) {
            if (!isIgnored(ignore, dep)) {
                for (CppArtifact cpp : dep.cppDependencies) {
                    if (cpp.headerClassifier != null)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_headers", bin, dse, cpp.skipInvalidPlatforms));
                    if (cpp.sourcesClassifier != null)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_sources", bin, dse, cpp.skipInvalidPlatforms));
                    if (cpp.binaryPlatforms != null && cpp.binaryPlatforms.length > 0)
                        dds.add(new DelegatedDependencySet(dep.uuid + cpp.libName + "_binaries", bin, dse, cpp.skipInvalidPlatforms));
                }
            }
        }

        for (DelegatedDependencySet set : dds) {
            bin.lib(set);
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
        String configuration;

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
