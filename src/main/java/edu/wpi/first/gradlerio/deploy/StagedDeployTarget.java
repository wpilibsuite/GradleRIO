package edu.wpi.first.gradlerio.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;

import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.artifact.Artifact;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public abstract class StagedDeployTarget extends RemoteTarget {
    private final Map<DeployStage, TaskProvider<StageDeployTask>> previousStageMap = new HashMap<>();

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
            previousStageMap.put(stage, previousStage);
            previousStage = stageTask;
        }
        TaskProvider<StageDeployTask> lastStage = previousStage;
        getDeployTask().configure(x -> x.dependsOn(lastStage));

        getArtifacts().all(this::configureArtifact);
    }

    private void configureArtifact(Artifact artifact) {
        Callable<TaskProvider<StageDeployTask>> cbl = () -> computeArtifactStageDep(artifact);
        artifact.dependsOn(cbl);
    }

    private final DeployStage defaultStage = DeployStage.FileDeploy;

    private TaskProvider<StageDeployTask> computeArtifactStageDep(Artifact artifact) {
        // Artifact must depend on stage before
        DeployStage stage = defaultStage;
        if (artifact instanceof ExtensionAware) {
            ExtensionAware ext = (ExtensionAware)artifact;
            DeployStage innerStage = ext.getExtensions().findByType(DeployStage.class);
            if (innerStage != null) {
                stage = innerStage;
            }
        }
        TaskProvider<StageDeployTask> prevStageTask = previousStageMap.get(stage);
        return prevStageTask;
    }

    private List<Object> computeStageDependencies(StageDeployTask stageTask) {
        List<Object> depTasks = new ArrayList<>();
        for (Artifact artifact : getArtifacts()) {
            DeployStage stage = defaultStage;
            if (artifact instanceof ExtensionAware) {
                ExtensionAware ext = (ExtensionAware)artifact;
                DeployStage innerStage = ext.getExtensions().findByType(DeployStage.class);
                if (innerStage != null) {
                    stage = innerStage;
                }
            }
            if (stage == stageTask.getStage()) {
                depTasks.add(artifact.getDeployTask());
            }
        }
        return depTasks;
    }
}
