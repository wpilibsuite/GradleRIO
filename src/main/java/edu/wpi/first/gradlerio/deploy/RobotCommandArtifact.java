package edu.wpi.first.gradlerio.deploy;

import java.util.function.Function;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.embeddedtools.deploy.artifact.FileArtifact;
import edu.wpi.first.embeddedtools.deploy.context.DeployContext;

public class RobotCommandArtifact extends FileArtifact {

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
    public void deploy(DeployContext ctx) {
        String content = startCommandFunc.apply(ctx);

        ctx.execute("echo '" + content + "' > /home/lvuser/robotCommand");
        ctx.execute("chmod +x /home/lvuser/robotCommand; chown lvuser /home/lvuser/robotCommand");
    }

}
