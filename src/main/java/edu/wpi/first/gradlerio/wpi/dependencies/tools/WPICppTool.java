package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

// import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class WPICppTool implements Named {
    //TaskProvider<CppToolInstallTask> toolInstallTask
    private final TaskProvider<CppToolRunTask> toolRunTask;

    private final String name;

    private final Provider<String> version;

    public WPICppTool(Project project, String name, Provider<String> version, String artifactId) {
        // Configuration config = project.getConfigurations().getByName("wpiCppTools");
        // String toolsClassifier = project.getExtensions().getByType(WPIExtension.class).getCppToolsClassifier();
        // Provider<String> fullId = project.getProviders().provider(() -> {
        //     String id = artifactId;
        //     id += ":" + version.get();
        //     id += ":" + toolsClassifier + "@zip";
        //     return id;
        // });
        //Dependency dependency = project.getDependencies().add("wpiCppTools", artifactId);
        //toolInstallTask = project.tasks.register("${name}Install".toString(), CppToolInstallTask, name, config, dependency)
        toolRunTask = project.getTasks().register(name, CppToolRunTask.class, name);
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
