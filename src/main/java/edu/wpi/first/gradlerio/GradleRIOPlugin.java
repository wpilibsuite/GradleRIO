package edu.wpi.first.gradlerio;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.tasks.wrapper.Wrapper;

import edu.wpi.first.embeddedtools.EmbeddedTools;
import edu.wpi.first.gradlerio.deploy.FRCPlugin;
import edu.wpi.first.gradlerio.test.TestPlugin;

import java.util.Map;

import org.apache.log4j.Logger;
import edu.wpi.first.gradlerio.wpi.WPIPlugin;

public class GradleRIOPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getConfigurations().maybeCreate("nativeLib");
        project.getConfigurations().maybeCreate("nativeZip");

        project.getConfigurations().maybeCreate("nativeDesktopLib");
        project.getConfigurations().maybeCreate("nativeDesktopZip");

        project.getConfigurations().maybeCreate("nativeRaspbianLib");
        project.getConfigurations().maybeCreate("nativeRaspbianZip");

        project.getConfigurations().maybeCreate("nativeAarch64BionicLib");
        project.getConfigurations().maybeCreate("nativeAarch64BionicZip");

        project.getPluginManager().apply(EmbeddedTools.class);
        project.getPluginManager().apply(FRCPlugin.class);
        project.getPluginManager().apply(WPIPlugin.class);
        //project.getPluginManager().apply(ClionPlugin.class);
        //project.getPluginManager().apply(IDEPlugin.class);
        project.getPluginManager().apply(TestPlugin.class);

        project.getTasks().register("downloadAll", DownloadAllTask.class, t -> {
            t.setGroup("GradleRIO");
            t.setDescription("Download all dependencies that may be used by this project");
        });

        project.getTasks().withType(Wrapper.class).configureEach(wrapper -> {
            if (!project.hasProperty("no-gradlerio-wrapper")) {
                wrapper.setDistributionPath("permwrapper/dists");
                wrapper.setArchivePath("permwrapper/dists");
            }
        });

        // disableCacheCleanup();

        // project.getGradle().getTaskGraph().whenReady(graph -> {
        //     try {
        //         if (!project.hasProperty("skip-inspector"))
        //             inspector(project);
        //     } catch (Exception e) {
        //         Logger.getLogger(this.getClass()).info("Inspector failed: " + e.getMessage());
        //     }
        //     ensureSingletons(project, graph);
        // });


    }

    public static Action<Manifest> javaManifest(String robotMainClass) {
        return mf -> {
            mf.attributes(Map.of("Main-Class", robotMainClass));
        };
    }
}
