package edu.wpi.first.gradlerio.deploy;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

import edu.wpi.first.embeddedtools.EmbeddedTools;
import edu.wpi.first.embeddedtools.deploy.DeployExtension;
import edu.wpi.first.embeddedtools.deploy.artifact.ArtifactsExtension;
import edu.wpi.first.embeddedtools.deploy.context.DeployContext;
import edu.wpi.first.embeddedtools.deploy.target.TargetsExtension;

public class FRCPlugin implements Plugin<Project> {

    private Project project;

    public static final String LIB_DEPLOY_DIR = "/usr/local/frc/third-party/lib";

    @Override
    public void apply(Project project) {
        this.project = project;

        project.getPluginManager().apply(EmbeddedTools.class);
        // TODO
        //project.getPluginManager().apply(RioLogPlugin.class);

        project.afterEvaluate(p -> {

        });

        project.getExtensions().create("frc", FRCExtension.class, project);

        DeployExtension deployExtension = project.getExtensions().getByType(DeployExtension.class);
        ExtensionAware artifactsExt = (ExtensionAware)deployExtension.getArtifacts();
        ExtensionAware targetsExt = (ExtensionAware)deployExtension.getTargets();

        ArtifactsExtension artifacts = deployExtension.getArtifacts();
        TargetsExtension targets = deployExtension.getTargets();

        // TODO figure out how to do this
        //artifactsExt.getExtensions().add("frcJavaArtifact");
        targetsExt.getExtensions().create("frc", FRCTargetsExtension.class, targets);
        artifactsExt.getExtensions().create("frc", FRCArtifactsExtension.class, artifacts);


    }

    public Project getProject() {
        return project;
    }

    public static void ownDirectory(DeployContext ctx, String directory) {
        ctx.execute("chmod -R 777 \"" + directory + "\" || true; chown -R lvuser:ni \"" + directory + "\"");
    }

    public static DeployExtension deployExtension(Project project) {
        return project.getExtensions().getByType(DeployExtension.class);
    }

}
