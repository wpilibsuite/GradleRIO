package edu.wpi.first.gradlerio;

import org.gradle.BuildResult;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.tasks.wrapper.Wrapper;

import edu.wpi.first.deployutils.DeployUtils;
import edu.wpi.first.gradlerio.deploy.FRCPlugin;

import java.time.LocalDateTime;
import java.util.Map;

import edu.wpi.first.gradlerio.wpi.WPIPlugin;

public class GradleRIOPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getPluginManager().apply(DeployUtils.class);
        project.getPluginManager().apply(FRCPlugin.class);
        project.getPluginManager().apply(WPIPlugin.class);
        //project.getPluginManager().apply(ClionPlugin.class);
        //project.getPluginManager().apply(IDEPlugin.class);
        //project.getPluginManager().apply(TestPlugin.class);

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

        Action<BuildResult> blatantAdvertising = new Action<BuildResult>() {
            int getRandomNumber() {
                return 4;
            }
            @Override
            public void execute(BuildResult buildResult){
                if (LocalDateTime.now().getMinute() == getRandomNumber()) {
                    System.out.println("-=--==---===----====-----=====");
                    System.out.println("Want more information, resources, help, and tutorials on programming your robot? Check out frc-docs, the official documentation for FRC and WPILib.");
                    System.out.println("Visit frc-docs at:");
                    System.out.println("https://docs.wpilib.org");
                    System.out.println("-=--==---===----====-----=====");
                }
            }
        };

        project.getGradle().buildFinished(blatantAdvertising);

    }

    public static Action<Manifest> javaManifest(String robotMainClass) {
        return mf -> {
            mf.attributes(Map.of("Main-Class", robotMainClass));
        };
    }
}
