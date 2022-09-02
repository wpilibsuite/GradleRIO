package edu.wpi.first.gradlerio.wpi;

import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.attributes.Attribute;

import edu.wpi.first.deployutils.log.ETLogger;
import edu.wpi.first.deployutils.log.ETLoggerFactory;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIDependenciesPlugin;
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolsPlugin;
import edu.wpi.first.nativeutils.UnzipTransform;
import edu.wpi.first.nativeutils.vendordeps.WPIVendorDepsExtension.VendorMavenRepo;

public class WPIPlugin implements Plugin<Project> {
    public static final Attribute<String> NATIVE_ARTIFACT_FORMAT = Attribute.of("artifactType", String.class);
    public static final String NATIVE_ARTIFACT_ZIP_TYPE = "zip";
    public static final String NATIVE_ARTIFACT_DIRECTORY_TYPE = "gr-directory";

    private ETLogger logger;

    public ETLogger getLogger() {
        return logger;
    }

    @Override
    public void apply(Project project) {
        // Apply transformation for JNI and simulation artifacts
        project.getDependencies().registerTransform(UnzipTransform.class, variantTransform -> {
            variantTransform.getFrom().attribute(NATIVE_ARTIFACT_FORMAT, NATIVE_ARTIFACT_ZIP_TYPE);
            variantTransform.getTo().attribute(NATIVE_ARTIFACT_FORMAT, NATIVE_ARTIFACT_DIRECTORY_TYPE);
        });

        WPIExtension wpiExtension = project.getExtensions().create("wpi", WPIExtension.class, project);
        logger = ETLoggerFactory.INSTANCE.create(this.getClass().getSimpleName());

        project.getPluginManager().apply(WPIToolsPlugin.class);
        project.getPluginManager().apply(WPIDependenciesPlugin.class);

        project.getTasks().register("wpiVersions", task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Print all versions of the wpi block");
            task.doLast(new Action<Task>() {
                @Override
                public void execute(Task arg0) {
                    // wpiExtension.versions().each { String key, Tuple tup ->
                    // println "${tup.first()}: ${tup[1]} (${key})"
                    // }
                }
            });
            // task.doLast {

            // }
        });

        project.getTasks().register("explainRepositories", task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Explain all Maven Repos present on this project");
            task.doLast(new Action<Task>() {

                @Override
                public void execute(Task arg0) {
                    explainRepositories(project);
                }

            });
        });

        project.afterEvaluate(ae -> {
            addMavenRepositories(project, wpiExtension);
        });
    }

    void explainRepositories(Project project) {
        for (MavenArtifactRepository repo : project.getRepositories().withType(MavenArtifactRepository.class)) {
            System.out.println(repo.getName() + " -> " + repo.getUrl());
        }
    }

    void addMavenRepositories(Project project, WPIExtension wpi) {
        boolean enableGroupLimits = wpi.getMaven().isEnableRepositoryGroupLimits();

        for(VendorMavenRepo repo : wpi.getVendor().getVendorRepos()) {
            wpi.getMaven().vendor(repo.getName(), config -> {
                config.setRelease(repo.getUrl());
                config.setAllowedGroupIds(repo.getAllowedGroupIds());
            });
            wpi.getMaven().getVendorCacheGroupIds().addAll(repo.getAllowedGroupIds());
        }

        wpi.getMaven().mirror("Official", mirror -> {
            mirror.setRelease("https://frcmaven.wpi.edu/artifactory/release");
            mirror.setDevelopment("https://frcmaven.wpi.edu/artifactory/development");
            mirror.setPriority(WPIMavenRepo.PRIORITY_OFFICIAL);
            mirror.setAllowedGroupIdsRegex(Set.of("edu\\.wpi\\.first\\..*"));
        });

        Set<String> vendorCacheIds = wpi.getMaven().getVendorCacheGroupIds();
        if (wpi.getMaven().isUseFrcMavenVendorCache() && (!vendorCacheIds.isEmpty() || !enableGroupLimits)) {
            wpi.getMaven().repo("FRCMavenVendorCache", cache -> {
                cache.setRelease("https://frcmaven.wpi.edu/artifactory/vendor-mvn-release");
                cache.setPriority(WPIMavenRepo.PRIORITY_WPILIB_VENDOR_CACHE);
                cache.setAllowedGroupIds(vendorCacheIds);
            });
        }

        if (wpi.getMaven().isUseLocal()) {
            project.getRepositories().maven(repo -> {
                repo.setName("WPILocal");
                repo.setUrl(project.getExtensions().getByType(WPIExtension.class).getFrcHome() + "/maven");
            });
        }

        if (wpi.getMaven().isUseFrcMavenLocalDevelopment()) {
            project.getRepositories().maven(repo -> {
                repo.setName("FRCDevelopmentLocal");
                repo.setUrl(System.getProperty("user.home") + "/releases/maven/development");
            });
        }

        if (wpi.getMaven().isUseFrcMavenLocalRelease()) {
            project.getRepositories().maven(repo -> {
                repo.setName("FRCReleaseLocal");
                repo.setUrl(System.getProperty("user.home") + "/releases/maven/release");
            });
        }

        WPIMavenRepo[] sortedMirrors = wpi.getMaven().stream().sorted((a, b) -> a.getPriority() - b.getPriority())
                .toArray(WPIMavenRepo[]::new);

        // If enabled, the development branch should have a higher weight than the
        // release
        // branch.
        if (wpi.getMaven().isUseDevelopment()) {
            for (WPIMavenRepo mirror : sortedMirrors) {
                if (mirror.getDevelopment() != null) {
                    project.getRepositories().maven(repo -> {
                        repo.setName("WPI" + mirror.getName() + "Development");
                        repo.setUrl(mirror.getDevelopment());
                        if (enableGroupLimits) {
                            repo.content(desc -> {
                                if (mirror.getAllowedGroupIds() != null) {
                                    for (String group : mirror.getAllowedGroupIds()) {
                                        desc.includeGroup(group);
                                    }
                                }
                                if (mirror.getAllowedGroupIdsRegex() != null) {
                                    for (String group : mirror.getAllowedGroupIdsRegex()) {
                                        desc.includeGroupByRegex(group);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        }

        for (WPIMavenRepo mirror : sortedMirrors) {
            if (mirror.getRelease() != null) {
                project.getRepositories().maven(repo -> {
                    repo.setName("WPI" + mirror.getName() + "Release");
                    repo.setUrl(mirror.getRelease());
                    if (enableGroupLimits) {
                        repo.content(desc -> {
                            if (mirror.getAllowedGroupIds() != null) {
                                for (String group : mirror.getAllowedGroupIds()) {
                                    desc.includeGroup(group);
                                }
                            }
                            if (mirror.getAllowedGroupIdsRegex() != null) {
                                for (String group : mirror.getAllowedGroupIdsRegex()) {
                                    desc.includeGroupByRegex(group);
                                }
                            }
                        });
                    }
                });
            }
        }

        // Maven Central is needed for EJML and JUnit
        if (wpi.getMaven().isUseMavenCentral()) {
            project.getRepositories().mavenCentral();
        }
    }
}
