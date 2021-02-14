package edu.wpi.first.gradlerio.deploy;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.embeddedtools.deploy.artifact.Artifact;
import edu.wpi.first.embeddedtools.deploy.artifact.ArtifactDeployTask;
import edu.wpi.first.embeddedtools.deploy.target.RemoteTarget;

public abstract class StagedDeployTarget extends RemoteTarget {
    private static class TaskAndPreviousTaskPair {
        public TaskAndPreviousTaskPair(TaskProvider<Task> task, TaskProvider<Task> previousTask) {
            this.task = task;
            this.previousTask = previousTask;
        }

        public TaskProvider<Task> task;
        public TaskProvider<Task> previousTask;
    }

    private final Map<DeployStage, TaskAndPreviousTaskPair> stageMap = new HashMap<>();

    @Inject
    public StagedDeployTarget(String name, Project project) {
        super(name, project);
        TaskProvider<Task> previousStage = null;
        for (DeployStage stage : DeployStage.values()) {
            String taskName = "deployStage" + name + stage.toString();
            TaskProvider<Task> fixedPreviousStage = previousStage;
            TaskProvider<Task> stageTask = project.getTasks().register(taskName, task -> {
                task.setGroup("GradleRIO");
                task.setDescription("Deploy stage " + stage + " for " + name);
                if (fixedPreviousStage != null) {
                    task.dependsOn(fixedPreviousStage);
                }
            });
            stageMap.put(stage, new TaskAndPreviousTaskPair(stageTask, previousStage));
            previousStage = stageTask;
        }
        TaskProvider<Task> lastStage = previousStage;
        getDeployTask().configure(x -> x.dependsOn(lastStage));
    }

    private void insertForStage(DeployStage stage, TaskProvider<ArtifactDeployTask> task) {
        TaskAndPreviousTaskPair stageTasks = stageMap.get(stage);
        stageTasks.task.configure(x -> x.dependsOn(task));
        // Task needs to depend on stage before
        if (stageTasks.previousTask != null) {
            task.configure(x -> x.dependsOn(stageTasks.previousTask));
        }
    }

    @Override
    public void artifactAdded(Artifact artifact, TaskProvider<ArtifactDeployTask> task) {
        if (artifact instanceof ExtensionAware) {
            ExtensionAware ext = (ExtensionAware)artifact;
            DeployStage stage = ext.getExtensions().findByType(DeployStage.class);
            if (stage != null) {
                insertForStage(stage, task);
            } else {
                // Configure as stage FileDeploy
                insertForStage(DeployStage.FileDeploy, task);
            }
        } else {
            // Configure as stage FileDeploy
            insertForStage(DeployStage.FileDeploy, task);
        }

        super.artifactAdded(artifact, task);
    }
}
