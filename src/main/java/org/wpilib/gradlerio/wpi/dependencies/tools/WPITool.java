package org.wpilib.gradlerio.wpi.dependencies.tools;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.wpilib.gradlerio.wpi.WPIExtension;

public class WPITool implements Named {
    private final TaskProvider<ToolInstallTask> toolInstallTask;
    private final TaskProvider<ToolRunTask> toolRunTask;

    private final String name;

    private final Provider<String> version;

    public WPITool(Project project, String name, Provider<String> version, String groupId, String artifactName, boolean platformJars, Provider<Directory> toolsFolder) {
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
        toolInstallTask = project.getTasks().register(name + "Install".toString(), ToolInstallTask.class);
        toolInstallTask.configure(t -> {
            t.setDescription("Install the tool " + name);
            t.getToolsFolder().set(toolsFolder);
            t.getConfiguration().set(config);
            t.getToolName().set(name);
            t.getArtifactName().set(artifactName);
        });
        toolRunTask = project.getTasks().register(name, ToolRunTask.class);
        toolRunTask.configure(t -> {
            t.dependsOn(toolInstallTask);
            t.setDescription("Run the tool " + name);
            t.getToolsFolder().set(toolsFolder);
            t.getToolName().set(name);
        });
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
