package edu.wpi.first.gradlerio.wpi.dependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.Gson;

import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;

import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;

public abstract class WPIVendorDepsExtension {

    private final WPIExtension wpiExt;

    private final Map<String, JsonDependency> dependencies = new HashMap<>();

    public Map<String, JsonDependency> getDependenciesMap() {
        return dependencies;
    }

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

    public static boolean isIgnored(String[] ignore, JsonDependency dep) {
        for (String i : ignore) {
            if (i.equals(dep.name) || i.equals(dep.uuid)) {
                return true;
            }
        }
        return false;
    }

    public static class JavaArtifact {
        public String groupId;
        public String artifactId;
        public String version;
    }

    public static class JniArtifact {
        public String groupId;
        public String artifactId;
        public String version;

        public boolean isJar;

        public String[] validPlatforms;
        public boolean skipInvalidPlatforms;
    }

    public static class CppArtifact {
        public String groupId;
        public String artifactId;
        public String version;
        public String libName;

        public String headerClassifier;
        public String sourcesClassifier;
        public String[] binaryPlatforms;
        public boolean skipInvalidPlatforms;

        public boolean sharedLibrary;
    }

    public static class JsonDependency {
        public String name;
        public String version;
        public String uuid;
        public String[] mavenUrls;
        public String jsonUrl;
        public String fileName;
        public JavaArtifact[] javaDependencies;
        public JniArtifact[] jniDependencies;
        public CppArtifact[] cppDependencies;
    }

}
