package edu.wpi.first.gradlerio.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.artifact.Artifact;
import edu.wpi.first.deployutils.deploy.artifact.ArtifactDeployTask;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public abstract class StagedDeployTarget extends RemoteTarget {
    // private static class TaskAndPreviousTaskPair {
    //     public TaskAndPreviousTaskPair(TaskProvider<StageDeployTask> task, TaskProvider<StageDeployTask> previousTask) {
    //         this.task = task;
    //         this.previousTask = previousTask;
    //     }

    //     public TaskProvider<StageDeployTask> task;
    //     public TaskProvider<StageDeployTask> previousTask;
    // }

    // private final Map<DeployStage, TaskAndPreviousTaskPair> stageMap = new HashMap<>();

    @Inject
    public StagedDeployTarget(String name, Project project, DeployExtension de) {
        super(name, project, de);
        TaskProvider<StageDeployTask> previousStage = null;
        for (DeployStage stage : DeployStage.values()) {
            String taskName = "deployStage" + name + stage.toString();
            TaskProvider<StageDeployTask> fixedPreviousStage = previousStage;
            TaskProvider<StageDeployTask> stageTask = project.getTasks().register(taskName, StageDeployTask.class, task -> {
                task.setGroup("GradleRIO");
                task.setDescription("Deploy stage " + stage + " for " + name);

                task.setStage(stage);
                task.setPreviousStage(fixedPreviousStage);

                Callable<List<Object>> cbl = () -> computeStageDependencies(task);
                task.dependsOn(cbl);

                if (fixedPreviousStage != null) {
                    task.dependsOn(fixedPreviousStage);
                }
            });
            //stageMap.put(stage, new TaskAndPreviousTaskPair(stageTask, previousStage));
            previousStage = stageTask;
        }
        TaskProvider<StageDeployTask> lastStage = previousStage;
        getDeployTask().configure(x -> x.dependsOn(lastStage));
    }

    private List<Object> computeStageDependencies(StageDeployTask stageTask) {
        List<Object> depTasks = new ArrayList<>();
        for (Artifact artifact : getArtifacts()) {
            if (artifact instanceof ExtensionAware) {
                ExtensionAware ext = (ExtensionAware)artifact;
                DeployStage stage = ext.getExtensions().findByType(DeployStage.class);
                if (stage != null) {
                    if (stage == stageTask.getStage()) {
                        insertForStage(stageTask, artifact.getDeployTask(), depTasks);
                    }
                } else {
                    // Configure as stage FileDeploy
                    throw new GradleException("Not configured");
                    //insertForStage(DeployStage.FileDeploy, task);
                }
            } else {
                // Configure as stage FileDeploy
                throw new GradleException("Not configured 2");
                //insertForStage(DeployStage.FileDeploy, task);
            }
        }
        return depTasks;
    }

    private void insertForStage(StageDeployTask stageTask, TaskProvider<ArtifactDeployTask> task, List<Object> depTasks) {
        System.out.println(stageTask.getName() + " is depending on " + task.getName());
        depTasks.add(task);
        // Task needs to depend on stage before
        if (stageTask.getPreviousStage() != null) {
            System.out.println(task.getName() + " is depending on " + stageTask.getPreviousStage());
            task.configure(x -> x.dependsOn(stageTask.getPreviousStage()));
        }
    }
}
