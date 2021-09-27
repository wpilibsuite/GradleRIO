package edu.wpi.first.gradlerio.deploy;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskProvider;

public abstract class StageDeployTask extends DefaultTask {
    private DeployStage stage;
    private TaskProvider<StageDeployTask> previousStage;

    @Internal
    public DeployStage getStage() {
        return stage;
    }
    public void setStage(DeployStage stage) {
        this.stage = stage;
    }

    @Internal
    public TaskProvider<StageDeployTask> getPreviousStage() {
        return previousStage;
    }

    public void setPreviousStage(TaskProvider<StageDeployTask> previousStage) {
        this.previousStage = previousStage;
    }

}
