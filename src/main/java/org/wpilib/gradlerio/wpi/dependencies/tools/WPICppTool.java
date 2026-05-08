package org.wpilib.gradlerio.wpi.dependencies.tools;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.wpilib.gradlerio.wpi.WPIExtension;

public class WPICppTool implements Named {
    //TaskProvider<CppToolInstallTask> toolInstallTask
    private final TaskProvider<CppToolRunTask> toolRunTask;

    private final String name;

    private final Provider<String> version;

    public WPICppTool(Project project, String name, Provider<String> version, String artifactId, Provider<Directory> toolsFolder) {
        //Configuration config = project.getConfigurations().getByName("wpiCppTools");
        String toolsClassifier = project.getExtensions().getByType(WPIExtension.class).getCppToolsClassifier();
        Provider<String> fullId = project.getProviders().provider(() -> {
            String id = artifactId;
            id += ":" + version.get();
            id += ":" + toolsClassifier + "@zip";
            return id;
        });
        project.getDependencies().add("wpiCppTools", fullId);
        //toolInstallTask = project.tasks.register("${name}Install".toString(), CppToolInstallTask, name, config, dependency)
        toolRunTask = project.getTasks().register(name, CppToolRunTask.class);
        toolRunTask.configure(t -> {
            t.setDescription("Run the tool " + name);
            t.getToolsFolder().set(toolsFolder);
            t.getToolName().set(name);
        });
        this.name = name;
        this.version = version;
    }

    public TaskProvider<CppToolRunTask> getToolRunTask() {
        return toolRunTask;
    }

    public String getName() {
        return name;
    }

    public Provider<String> getVersion() {
        return version;
    }
}
