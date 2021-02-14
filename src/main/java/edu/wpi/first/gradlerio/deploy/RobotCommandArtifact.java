package edu.wpi.first.gradlerio.deploy;

import java.util.function.Function;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.embeddedtools.deploy.artifact.AbstractArtifact;
import edu.wpi.first.embeddedtools.deploy.context.DeployContext;

public class RobotCommandArtifact extends AbstractArtifact {

    private Function<DeployContext, String> startCommandFunc;

    @Inject
    public RobotCommandArtifact(String name, Project project) {
        super(name, project);

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    public Function<DeployContext, String> getStartCommandFunc() {
        return startCommandFunc;
    }

    public void setStartCommandFunc(Function<DeployContext, String> startCommandFunc) {
        this.startCommandFunc = startCommandFunc;
    }

    @Override
    public void deploy(DeployContext arg0) {
        // TODO Auto-generated method stub
        // ctx.execute("chmod +x /home/lvuser/robotCommand")
    }

}
