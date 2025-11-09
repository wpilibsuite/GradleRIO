package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class WPIToolsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getConfigurations().maybeCreate("wpiTools");
        project.getConfigurations().maybeCreate("wpiCppTools");

        WPIExtension wpi = project.getExtensions().getByType(WPIExtension.class);
        List<WPITool> tools = new ArrayList<>();
        List<WPICppTool> cppTools = new ArrayList<>();

        Provider<Directory> frcHome = wpi.getFrcHome();
        Provider<Directory> toolsFolder = project.provider(() -> frcHome.get().dir("tools"));

        cppTools.add(new WPICppTool(project, "OutlineViewer", wpi.getVersions().getOutlineViewerVersion(),
                "org.wpilib.tools:OutlineViewer", toolsFolder));
        cppTools.add(
                new WPICppTool(project, "Glass", wpi.getVersions().getGlassVersion(), "org.wpilib.tools:Glass",
                        toolsFolder));
        cppTools.add(
                new WPICppTool(project, "SysId", wpi.getVersions().getSysIdVersion(), "org.wpilib.tools:SysId",
                        toolsFolder));
        cppTools.add(new WPICppTool(project, "DataLogTool", wpi.getVersions().getDataLogToolVersion(),
                "org.wpilib.tools:DataLogTool", toolsFolder));

        project.getTasks().register("InstallAllTools", task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Install All Tools");

            for (WPITool tool : tools) {
                task.dependsOn(tool.getToolInstallTask());
            }
        });
    }
}
