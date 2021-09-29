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
        List<WPITool> tools = new ArrayList<>();
        List<WPICppTool> cppTools = new ArrayList<>();

        String frcHome = wpi.getFrcHome();
        File toolFolder = new File(frcHome, "tools");

        ToolInstallTask.setToolsFolder(toolFolder);
        tools.add(new WPITool(project, "SmartDashboard", wpi.getVersions().getSmartDashboardVersion(), "edu.wpi.first.tools" , "SmartDashboard", true));
        tools.add(new WPITool(project, "ShuffleBoard", wpi.getVersions().getShuffleboardVersion(), "edu.wpi.first.shuffleboard" , "shuffleboard", true));
        tools.add(new WPITool(project, "RobotBuilder", wpi.getVersions().getRobotBuilderVersion(), "edu.wpi.first.tools" , "RobotBuilder", false));
        tools.add(new WPITool(project, "RobotBuilder-Old", wpi.getVersions().getRobotBuilderOldVersion(), "edu.wpi.first.tools" , "RobotBuilder-Old", false));
        tools.add(new WPITool(project, "PathWeaver", wpi.getVersions().getPathWeaverVersion(), "edu.wpi.first.tools" , "PathWeaver", true));

        cppTools.add(new WPICppTool(project, "OutlineViewer", wpi.getVersions().getOutlineViewerVersion(), "edu.wpi.first.tools:OutlineViewer"));
        cppTools.add(new WPICppTool(project, "Glass", wpi.getVersions().getGlassVersion(), "edu.wpi.first.tools:Glass"));
        cppTools.add(new WPICppTool(project, "SysId", wpi.getVersions().getSysIdVersion(), "edu.wpi.first.tools:SysId"));

        project.getTasks().register("InstallAllTools", task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Install All Tools");

            for (WPITool tool : tools) {
                task.dependsOn(tool.getToolInstallTask());
            }
        });
    }
}
