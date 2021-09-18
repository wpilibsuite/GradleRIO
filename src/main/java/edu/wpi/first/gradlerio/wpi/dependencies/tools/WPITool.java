package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import edu.wpi.first.gradlerio.wpi.WPIExtension;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class WPITool implements Named {
    private final TaskProvider<ToolInstallTask> toolInstallTask;
    private final TaskProvider<ToolRunTask> toolRunTask;

    private final String name;

    private final Provider<String> version;

    public WPITool(Project project, String name, Provider<String> version, String groupId, String artifactName, boolean platformJars) {
        Configuration config = project.getConfigurations().getByName("wpiTools");
        String toolsClassifier = project.getExtensions().getByType(WPIExtension.class).getToolsClassifier();
        Provider<String> fullId = project.getProviders().provider(() -> {
            String id = groupId + ":" + artifactName;
            id += ":" + version.get();
            if (platformJars) {
                id += ":" + toolsClassifier;
            }
            return id;
        });
        project.getDependencies().add("wpiTools", fullId);
        toolInstallTask = project.getTasks().register(name + "Install".toString(), ToolInstallTask.class, name, config, artifactName);
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

    public Provider<String> getVersion() {
        return version;
    }
}
