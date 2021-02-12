package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import edu.wpi.first.gradlerio.wpi.WPIExtension;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;

public class WPITool implements Named {
    private final TaskProvider<ToolInstallTask> toolInstallTask;
    private final TaskProvider<ToolRunTask> toolRunTask;

    private final String name;

    private final String version;

    public WPITool(Project project, String name, String version, String artifactId, boolean platformJars) {
        Configuration config = project.getConfigurations().getByName("wpiTools");
        String toolsClassifier = project.getExtensions().getByType(WPIExtension.class).getToolsClassifier();
        if (platformJars) {
            artifactId += ":" + toolsClassifier;
        }
        Dependency dependency = project.getDependencies().add("wpiTools", artifactId);
        toolInstallTask = project.getTasks().register(name + "Install".toString(), ToolInstallTask.class, name, config, dependency);
        toolRunTask = project.getTasks().register(name, ToolRunTask.class, name, toolInstallTask);
        this.name = name;
        this.version = version;
    }

    public TaskProvider<ToolInstallTask> getToolInstallTask() {
        return toolInstallTask;
    }

    public TaskProvider<ToolRunTask> getToolRunTask() {
        return toolRunTask;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
