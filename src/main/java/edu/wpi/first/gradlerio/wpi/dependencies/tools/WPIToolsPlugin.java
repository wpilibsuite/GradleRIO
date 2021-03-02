package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class WPIToolsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getConfigurations().maybeCreate("wpiTools");
        project.getConfigurations().maybeCreate("wpiCppTools");

        WPIExtension wpi = project.getExtensions().getByType(WPIExtension.class);
        project.afterEvaluate(p -> {
            List<WPITool> tools = new ArrayList<>();
            List<WPICppTool> cppTools = new ArrayList<>();

            String frcHome = wpi.getFrcHome();
            File toolFolder = new File(frcHome, "tools");

            ToolInstallTask.setToolsFolder(toolFolder);
            // TODO fix me lazy
            tools.add(new WPITool(project, "SmartDashboard", wpi.getVersions().getSmartDashboardVersion().get(), "edu.wpi.first.tools:SmartDashboard:" + wpi.getVersions().getSmartDashboardVersion().get(), true));
            tools.add(new WPITool(project, "ShuffleBoard", wpi.getVersions().getShuffleboardVersion().get(), "edu.wpi.first.shuffleboard:shuffleboard:" + wpi.getVersions().getShuffleboardVersion().get(), true));
            tools.add(new WPITool(project, "OutlineViewer", wpi.getVersions().getOutlineViewerVersion().get(), "edu.wpi.first.tools:OutlineViewer:" + wpi.getVersions().getOutlineViewerVersion().get(), true));
            tools.add(new WPITool(project, "RobotBuilder", wpi.getVersions().getRobotBuilderVersion().get(), "edu.wpi.first.tools:RobotBuilder:" + wpi.getVersions().getRobotBuilderVersion().get(), false));
            tools.add(new WPITool(project, "RobotBuilder-Old", wpi.getVersions().getRobotBuilderOldVersion().get(), "edu.wpi.first.tools:RobotBuilder-Old:" + wpi.getVersions().getRobotBuilderOldVersion().get(), false));
            tools.add(new WPITool(project, "PathWeaver", wpi.getVersions().getPathWeaverVersion().get(), "edu.wpi.first.tools:PathWeaver:" + wpi.getVersions().getPathWeaverVersion().get(), true));
            cppTools.add(new WPICppTool(project, "Glass", wpi.getVersions().getGlassVersion().get(), "edu.wpi.first.tools:Glass:" + wpi.getVersions().getGlassVersion().get()));

            project.getTasks().register("InstallAllTools", task -> {
                task.setGroup("GradleRIO");
                task.setDescription("Install All Tools");

                for (WPITool tool : tools) {
                    task.dependsOn(tool.getToolInstallTask());
                }
            });
        });
    }
}
