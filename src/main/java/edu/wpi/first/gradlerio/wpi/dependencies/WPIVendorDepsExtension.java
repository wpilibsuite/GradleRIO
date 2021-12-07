package edu.wpi.first.gradlerio.wpi.dependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.gson.Gson;

import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;

import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.WPIMavenRepo;
import edu.wpi.first.gradlerio.wpi.WPIVersionsExtension;

public abstract class WPIVendorDepsExtension {

    private final WPIExtension wpiExt;

    private final NamedDomainObjectSet<NamedJsonDependency> dependencySet;

    public NamedDomainObjectSet<NamedJsonDependency> getDependencySet() {
        return dependencySet;
    }

    private final ETLogger log;
    private final Gson gson = new Gson();

    public static final String DEFAULT_VENDORDEPS_FOLDER_NAME = "vendordeps";
    public static final String GRADLERIO_VENDOR_FOLDER_PROPERTY = "gradlerio.vendordep.folder.path";

    @Inject
    public WPIVendorDepsExtension(WPIExtension wpiExt) {
        this.wpiExt = wpiExt;
        this.log = ETLoggerFactory.INSTANCE.create("WPIVendorDeps");
        dependencySet = wpiExt.getProject().getObjects().namedDomainObjectSet(NamedJsonDependency.class);
    }

    private File vendorFolder(Project project) {
        Object prop = project.findProperty(GRADLERIO_VENDOR_FOLDER_PROPERTY);
        String filepath = DEFAULT_VENDORDEPS_FOLDER_NAME;
        if (prop != null && !prop.equals(DEFAULT_VENDORDEPS_FOLDER_NAME)) {
            log.logErrorHead(
                    "Warning! You have the property " + GRADLERIO_VENDOR_FOLDER_PROPERTY + " set to a non-default value: " + prop);
            log.logError("The default path (from the project root) is " + DEFAULT_VENDORDEPS_FOLDER_NAME);
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
        if (dependencySet.findByName(dep.uuid) != null) {
            return;
        }

        NamedJsonDependency namedDep = new NamedJsonDependency(dep.uuid, dep);
        dependencySet.add(namedDep);

        if (dep.mavenUrls != null) {
            // Enumerate all group ids
            Set<String> groupIds = new HashSet<>();
            for (CppArtifact cpp : dep.cppDependencies) {
                groupIds.add(cpp.groupId);
            }
            for (JniArtifact jni : dep.jniDependencies) {
                groupIds.add(jni.groupId);
            }
            for (JavaArtifact java : dep.javaDependencies) {
                groupIds.add(java.groupId);
            }
            if (dep.extraGroupIds != null) {
                for (String groupId : dep.extraGroupIds) {
                    groupIds.add(groupId);
                }
            }

            int i = 0;
            for (String url : dep.mavenUrls) {
                boolean found = false;

                for (WPIMavenRepo machingRepo : wpiExt.getMaven().matching(x -> x.getRelease().equals(url))) {
                    found = true;
                    machingRepo.getAllowedGroupIds().addAll(groupIds);
                }

                // Only add if the maven doesn"t yet exist.
                if (!found) {
                    String name = dep.uuid + "_" + i++;
                    log.info("Registering vendor dep maven: " + name + " on project " + wpiExt.getProject().getPath());
                    boolean allowsCache = dep.mavenUrlsInWpilibCache != null ? dep.mavenUrlsInWpilibCache : false;
                    if (allowsCache) {
                        wpiExt.getMaven().getVendorCacheGroupIds().addAll(groupIds);
                    }
                    wpiExt.getMaven().vendor(name, repo -> {
                        repo.setRelease(url);
                        repo.setAllowedGroupIds(groupIds);
                    }, allowsCache);
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
        public Boolean mavenUrlsInWpilibCache;
        public String[] mavenUrls;
        public String[] extraGroupIds;
        public String jsonUrl;
        public String fileName;
        public JavaArtifact[] javaDependencies;
        public JniArtifact[] jniDependencies;
        public CppArtifact[] cppDependencies;
    }

    public static class NamedJsonDependency implements Named {
        private final JsonDependency dependency;
        private final String name;

        public NamedJsonDependency(String name, JsonDependency dependency) {
            this.name = name;
            this.dependency = dependency;
        }

        public String getName() {
            return name;
        }

        public JsonDependency getDependency() {
            return dependency;
        }
    }

}
