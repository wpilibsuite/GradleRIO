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
            tools.add(new WPITool(project, "SmartDashboard", wpi.getSmartDashboardVersion(), "edu.wpi.first.tools:SmartDashboard:" + wpi.getSmartDashboardVersion(), true));
            tools.add(new WPITool(project, "ShuffleBoard", wpi.getShuffleboardVersion(), "edu.wpi.first.shuffleboard:shuffleboard:" + wpi.getShuffleboardVersion(), true));
            tools.add(new WPITool(project, "OutlineViewer", wpi.getOutlineViewerVersion(), "edu.wpi.first.tools:OutlineViewer:" + wpi.getOutlineViewerVersion(), true));
            tools.add(new WPITool(project, "RobotBuilder", wpi.getRobotBuilderVersion(), "edu.wpi.first.tools:RobotBuilder:" + wpi.getRobotBuilderVersion(), false));
            tools.add(new WPITool(project, "RobotBuilder-Old", wpi.getRobotBuilderOldVersion(), "edu.wpi.first.tools:RobotBuilder-Old:" + wpi.getRobotBuilderOldVersion(), false));
            tools.add(new WPITool(project, "PathWeaver", wpi.getPathWeaverVersion(), "edu.wpi.first.tools:PathWeaver:" + wpi.getPathWeaverVersion(), true));
            cppTools.add(new WPICppTool(project, "Glass", wpi.getGlassVersion(), "edu.wpi.first.tools:Glass:" + wpi.getGlassVersion()));

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
