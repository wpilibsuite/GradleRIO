package edu.wpi.first.gradlerio.deploy;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

import edu.wpi.first.deployutils.DeployUtils;
import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.artifact.Artifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.location.DeployLocation;
import edu.wpi.first.gradlerio.deploy.roborio.DSDeployLocation;
import edu.wpi.first.gradlerio.deploy.roborio.FRCJNILibraryArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.FRCJREArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.FRCJavaArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.FRCNativeArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.FRCProgramStartArtifact;
import edu.wpi.first.gradlerio.deploy.roborio.RoboRIO;
import edu.wpi.first.gradlerio.deploy.roborio.RobotCommandArtifact;
import edu.wpi.first.deployutils.deploy.NamedObjectFactory;

public class FRCDeployPlugin implements Plugin<Project> {

    private Project project;

    public static final String LIB_DEPLOY_DIR = "/usr/local/frc/third-party/lib";

    private void configureRoboRIOTypes(RoboRIO target) {
        ObjectFactory objects = target.getProject().getObjects();
        ExtensiblePolymorphicDomainObjectContainer<DeployLocation> locations = target.getLocations();
        ExtensiblePolymorphicDomainObjectContainer<Artifact> artifacts = target.getArtifacts();

        NamedObjectFactory.registerType(FRCJavaArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCNativeArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCJREArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCJNILibraryArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCProgramStartArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(RobotCommandArtifact.class, artifacts, target, objects);

        NamedObjectFactory.registerType(DSDeployLocation.class, locations, target, objects);
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
            configureRoboRIOTypes(target);
            return target;
        });
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
