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
import edu.wpi.first.gradlerio.deploy.systemcore.DSDeployLocation;
import edu.wpi.first.gradlerio.deploy.systemcore.FRCJNILibraryArtifact;
import edu.wpi.first.gradlerio.deploy.systemcore.FRCJavaArtifact;
import edu.wpi.first.gradlerio.deploy.systemcore.FRCNativeArtifact;
import edu.wpi.first.gradlerio.deploy.systemcore.RobotCommandArtifact;
import edu.wpi.first.gradlerio.deploy.systemcore.SystemCore;
import edu.wpi.first.deployutils.deploy.NamedObjectFactory;

public class FRCDeployPlugin implements Plugin<Project> {

    private Project project;

    public static final String LIB_DEPLOY_DIR = "/home/systemcore/frc/third-party/lib";

    private void configureSystemCoreTypes(SystemCore target) {
        ObjectFactory objects = target.getProject().getObjects();
        ExtensiblePolymorphicDomainObjectContainer<DeployLocation> locations = target.getLocations();
        ExtensiblePolymorphicDomainObjectContainer<Artifact> artifacts = target.getArtifacts();

        NamedObjectFactory.registerType(FRCJavaArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCNativeArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCJNILibraryArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(RobotCommandArtifact.class, artifacts, target, objects);

        NamedObjectFactory.registerType(DSDeployLocation.class, locations, target, objects);
    }

    @Override
    public void apply(Project project) {
        this.project = project;

        project.getPluginManager().apply(DeployUtils.class);
        // TODO
        //project.getPluginManager().apply(RioLogPlugin.class);

        DeployExtension deployExtension = project.getExtensions().getByType(DeployExtension.class);

        FRCExtension frcExtension = project.getExtensions().create("frc", FRCExtension.class, project, deployExtension);

        // Register the SystemCore target
        deployExtension.getTargets().registerFactory(SystemCore.class, name -> {
            SystemCore target = project.getObjects().newInstance(SystemCore.class, name, project, deployExtension, frcExtension);
            configureSystemCoreTypes(target);
            return target;
        });
    }

    public Project getProject() {
        return project;
    }

    public static void ownDirectory(DeployContext ctx, String directory) {
        // For now until we have a better setup, do nothing.
        //ctx.execute("chmod -R 777 \"" + directory + "\" || true; chown -R systemcore \"" + directory + "\"");
    }

    public static DeployExtension deployExtension(Project project) {
        return project.getExtensions().getByType(DeployExtension.class);
    }

}
