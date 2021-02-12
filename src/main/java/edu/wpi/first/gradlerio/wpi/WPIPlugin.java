package edu.wpi.first.gradlerio.wpi;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import edu.wpi.first.embeddedtools.log.ETLogger;
import edu.wpi.first.embeddedtools.log.ETLoggerFactory;
import edu.wpi.first.embeddedtools.toolchains.ToolchainsPlugin;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIDependenciesPlugin;
import edu.wpi.first.gradlerio.wpi.dependencies.WPINativeJsonDepRules;
import edu.wpi.first.gradlerio.wpi.dependencies.tools.WPIToolsPlugin;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;
import edu.wpi.first.nativeutils.NativeUtils;
import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.ToolchainExtension;
import edu.wpi.first.toolchain.ToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;
import edu.wpi.first.vscode.GradleVsCode;

class WPIPlugin implements Plugin<Project> {
    private ETLogger logger;

    public ETLogger getLogger() {
        return logger;
    }

    @Override
    public void apply(Project project) {
        WPIExtension wpiExtension = project.getExtensions().create("wpi", WPIExtension.class, project);
        project.getExtensions().create("sim", SimulationExtension.class, project);
        logger = ETLoggerFactory.INSTANCE.create(this.getClass().getSimpleName());

        project.getPluginManager().apply(WPIToolsPlugin.class);
        project.getPluginManager().apply(WPIDependenciesPlugin.class);

        project.getPlugins().withType(ToolchainsPlugin.class).all(p -> {
            logger.info("DeployTools Native Project Detected");
            project.getPluginManager().apply(ToolchainPlugin.class);
            project.getPluginManager().apply(RoboRioToolchainPlugin.class);
            project.getPluginManager().apply(NativeUtils.class);
            project.getPluginManager().apply(WPINativeCompileRules.class);

            NativeUtilsExtension nte = project.getExtensions().getByType(NativeUtilsExtension.class);
            nte.withRoboRIO();
            nte.addWpiNativeUtils();

            ToolchainExtension te = project.getExtensions().getByType(ToolchainExtension.class);
            te.getCrossCompilers().named(nte.getWpi().platforms.roborio, c -> {
                c.getOptional().set(false);
            });

            nte.getWpi().addWarnings();
            nte.setSinglePrintPerPlatform();

            project.afterEvaluate(ap -> {
                nte.getWpi().configureDependencies(deps -> {
                    deps.wpiVersion = wpiExtension.getWpilibVersion();
                    deps.niLibVersion = wpiExtension.getNiLibrariesVersion();
                    deps.opencvVersion = wpiExtension.getOpencvVersion();
                    deps.googleTestVersion = wpiExtension.getGoogleTestVersion();
                    deps.imguiVersion = wpiExtension.getImguiVersion();
                    deps.wpimathVersion = wpiExtension.getWpimathVersion();
                });
            });

            project.getPluginManager().apply(GradleVsCode.class);
            project.getPluginManager().apply(WPINativeJsonDepRules.class);
        });

        project.getTasks().register("wpiVersions", task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Print all versions of the wpi block");
            task.doLast(new Action<Task>() {
				@Override
				public void execute(Task arg0) {
                                //     wpiExtension.versions().each { String key, Tuple tup ->
            //         println "${tup.first()}: ${tup[1]} (${key})"
            //     }
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

        WPIMavenRepo[] sortedMirrors = wpi.getMaven().stream().sorted((a, b) -> a.getPriority() - b.getPriority()).toArray(WPIMavenRepo[]::new);

        // If enabled, the development branch should have a higher weight than the release
        // branch.
        if (wpi.getMaven().isUseDevelopment()) {
            for (WPIMavenRepo mirror : sortedMirrors) {
                if (mirror.getDevelopment() != null) {
                    project.getRepositories().maven(repo -> {
                        repo.setName("WPI" + mirror.getName() + "Development");
                        repo.setUrl(mirror.getDevelopment());
                    });
                }
            }
        }

        for (WPIMavenRepo mirror : sortedMirrors) {
            if (mirror.getDevelopment() != null) {
                project.getRepositories().maven(repo -> {
                    repo.setName("WPI" + mirror.getName() + "Release");
                    repo.setUrl(mirror.getRelease());
                });
            }
        }

        // Maven Central is needed for EJML and JUnit
        if (wpi.getMaven().isUseMavenCentral()) {
            project.getRepositories().mavenCentral();
        }
    }
}
