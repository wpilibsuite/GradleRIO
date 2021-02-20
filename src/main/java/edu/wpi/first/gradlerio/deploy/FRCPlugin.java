package edu.wpi.first.gradlerio.deploy;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;

import edu.wpi.first.deployutils.DeployUtils;
import edu.wpi.first.deployutils.deploy.DeployExtension;
//import edu.wpi.first.deployutils.deploy.artifact.ArtifactsExtension;
import edu.wpi.first.deployutils.deploy.artifact.Artifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
//import edu.wpi.first.deployutils.deploy.target.TargetsExtension;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.deployutils.deploy.target.location.DeployLocation;

public class FRCPlugin implements Plugin<Project> {

    private Project project;

    public static final String LIB_DEPLOY_DIR = "/usr/local/frc/third-party/lib";

    private void configureRoboRIOTypes(ExtensiblePolymorphicDomainObjectContainer<Artifact> artifacts, ExtensiblePolymorphicDomainObjectContainer<DeployLocation> locations, RemoteTarget target) {

        ObjectFactory objects = target.getProject().getObjects();
        artifacts.registerFactory(FRCJavaArtifact.class, name -> {
            var art = objects.newInstance(FRCJavaArtifact.class, name, target);
            return art;
        });
        artifacts.registerFactory(FRCNativeArtifact.class, name -> {
            var art = objects.newInstance(FRCNativeArtifact.class, name, target);
            return art;
        });
        artifacts.registerFactory(FRCJREArtifact.class, name -> {
            var art = objects.newInstance(FRCJREArtifact.class, name, target);
            return art;
        });

        artifacts.registerFactory(FRCProgramStartArtifact.class, name -> {
            var art = objects.newInstance(FRCProgramStartArtifact.class, name, target);
            return art;
        });
        artifacts.registerFactory(RobotCommandArtifact.class, name -> {
            var art = objects.newInstance(RobotCommandArtifact.class, name, target);
            return art;
        });



        locations.registerFactory(DSDeployLocation.class, name -> {
            return objects.newInstance(DSDeployLocation.class, name, target);
        });
    }

    @Override
    public void apply(Project project) {
        this.project = project;

        project.getPluginManager().apply(DeployUtils.class);
        // TODO
        //project.getPluginManager().apply(RioLogPlugin.class);

        project.getExtensions().create("frc", FRCExtension.class, project);

        DeployExtension deployExtension = project.getExtensions().getByType(DeployExtension.class);

        // Register the RoboRIO target
        deployExtension.getTargets().registerFactory(RoboRIO.class, name -> {
            RoboRIO target = project.getObjects().newInstance(RoboRIO.class, name, project, deployExtension);
            configureRoboRIOTypes(target.getArtifacts(), target.getLocations(), target);
            return target;
        });
        //ExtensionAware artifactsExt = (ExtensionAware)deployExtension.getArtifacts();
        //ExtensionAware targetsExt = (ExtensionAware)deployExtension.getTargets();

        //ArtifactsExtension artifacts = deployExtension.getArtifacts();
        //TargetsExtension targets = deployExtension.getTargets();

        // TODO figure out how to do this
        //artifactsExt.getExtensions().add("frcJavaArtifact");
        //targetsExt.getExtensions().create("frc", FRCTargetsExtension.class, targets);
        //artifactsExt.getExtensions().create("frc", FRCArtifactsExtension.class, artifacts);


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
