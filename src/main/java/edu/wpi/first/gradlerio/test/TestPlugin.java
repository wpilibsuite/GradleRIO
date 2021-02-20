package edu.wpi.first.gradlerio.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativeplatform.plugins.NativeComponentPlugin;

//import edu.wpi.first.deployutils.toolchains.ToolchainsPlugin;

public class TestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getConfigurations().maybeCreate("simulation");

        project.getPluginManager().apply(JavaTestPlugin.class);

        project.getPlugins().withType(NativeComponentPlugin.class).all(x -> project.getPluginManager().apply(NativeTestPlugin.class));

        project.getTasks().register("externalSimulate", ExternalSimulationMergeTask.class, t -> {
            t.dependsOn(project.getTasks().withType(ExternalSimulationTask.class));
        });
    }

    public static List<String> getHALExtensions(Project project) {
        Configuration cfg = project.getConfigurations().getByName("simulation");
        String ext = OperatingSystem.current().getSharedLibrarySuffix();
        Iterable<File> allFiles = () -> cfg.getDependencies().stream().map(x -> cfg.files(x)).flatMap(Set<File>::stream).iterator();

        List<String> rtLibs = new ArrayList<>();

        for (File f : allFiles) {
            if (f.getAbsolutePath().endsWith(".zip")) {
                project.zipTree(f)
                    .matching(pat -> pat.include("**/*" + ext)).getFiles()
                    .stream()
                    .map(x -> x.getAbsolutePath())
                    .forEachOrdered(rtLibs::add);
            } else if (f.isDirectory()) {
                project.fileTree(f)
                    .matching(pat -> pat.include("**/*" + ext)).getFiles()
                    .stream()
                    .map(x -> x.getAbsolutePath())
                    .forEachOrdered(rtLibs::add);
            } else {
                // Assume it's a native file already
                rtLibs.add(f.getAbsolutePath());
            }
        }
        return rtLibs;
    }

    public static String getHALExtensionsEnvVar(Project project) {
        List<String> rtLibs = getHALExtensions(project);
        return String.join(envDelimiter(), rtLibs);
    }

    public static Map<String, String> getSimLaunchEnv(Project project, String ldpath) {
        Map<String, String> env = new HashMap<>();
        env.put("HALSIM_EXTENSIONS", getHALExtensionsEnvVar(project));
        if (OperatingSystem.current().isUnix()) {
            env.put("LD_LIBRARY_PATH", ldpath);
            env.put("DYLD_FALLBACK_LIBRARY_PATH", ldpath);
            env.put("DYLD_LIBRARY_PATH", ldpath);
        } else if (OperatingSystem.current().isWindows()) {
            env.put("PATH", System.getenv("PATH") + envDelimiter() + ldpath);
        }
        return env;
    }

    public static String envDelimiter() {
        return OperatingSystem.current().isWindows() ? ";" : ":";
    }

}
