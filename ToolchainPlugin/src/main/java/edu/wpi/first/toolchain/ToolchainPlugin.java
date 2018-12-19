package edu.wpi.first.toolchain;

import jaci.gradle.toolchains.ToolchainsPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.internal.text.TreeFormatter;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ToolchainPlugin implements Plugin<Project> {
    // Necessary to have access to project.configurations and such in the RuleSource
    class ProjectWrapper {
        private Project project;

        public Project getProject() {
            return this.project;
        }

        ProjectWrapper(Project project) { this.project = project; }
    }

    private ToolchainExtension ext;

    @Override
    public void apply(Project project) {
        ext = project.getExtensions().create("toolchainsPlugin", ToolchainExtension.class, project);
        project.getExtensions().add("toolchainProjectWrapper", new ProjectWrapper(project));

        project.getTasks().register("explainToolchains", (Task t) -> {
            t.setGroup("Toolchains");
            t.setDescription("Explain Toolchains Plugin extension");

            t.doLast((task) -> {
                TreeFormatter formatter = new TreeFormatter();
                ext.explain(formatter);
                System.out.println(formatter.toString());
            });
        });

        ext.all((ToolchainDescriptor desc) -> {
            project.getTasks().register(desc.installTaskName(), InstallToolchainTask.class, (InstallToolchainTask t) -> {
                t.setGroup("Toolchains");
                t.setDescription("Install Toolchain for " + desc.getName() + " if installers are available.");
                t.setDescriptor(desc);
            });
        });

        project.getGradle().getTaskGraph().whenReady((TaskExecutionGraph graph) -> {
            // Sort into buckets based on the descriptor, then cancel all but the first entry in each
            // of those sublists, ensuring only one instance of each install task may run.
            List<InstallToolchainTask> installTasks = graph.getAllTasks().stream()
                    .filter(t -> t instanceof InstallToolchainTask)
                    .map(t -> (InstallToolchainTask) t)
                    .collect(Collectors.toList());

            installTasks.stream()
                    .collect(Collectors.groupingBy(InstallToolchainTask::getDescriptorName))
                    .values()
                    .forEach((list) -> {
                        list.stream().skip(1).forEach(t -> t.setEnabled(false));
                    });

            // Cancel all non-install tasks.
            if (installTasks.size() > 0)
                graph.getAllTasks().stream()
                    .filter(t -> !(t instanceof InstallToolchainTask))
                    .forEach(t -> {
                        System.out.println("Cancelling: " + t.getName());
                        t.setEnabled(false);
                    });
        });

        project.getPluginManager().apply(ToolchainRules.class);
        project.getPluginManager().apply(ToolchainsPlugin.class);
    }

    public static File gradleHome() {
        return new File(System.getProperty("user.home"), ".gradle");
    }

    public static File pluginHome() {
        return new File(gradleHome(), "toolchains");
    }

}
