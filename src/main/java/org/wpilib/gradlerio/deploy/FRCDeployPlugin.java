package org.wpilib.gradlerio.deploy;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

import org.wpilib.deployutils.DeployUtils;
import org.wpilib.deployutils.deploy.DeployExtension;
import org.wpilib.deployutils.deploy.artifact.Artifact;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.deployutils.deploy.target.location.DeployLocation;
import org.wpilib.gradlerio.deploy.systemcore.DSDeployLocation;
import org.wpilib.gradlerio.deploy.systemcore.FRCJNILibraryArtifact;
import org.wpilib.gradlerio.deploy.systemcore.FRCJavaArtifact;
import org.wpilib.gradlerio.deploy.systemcore.FRCNativeArtifact;
import org.wpilib.gradlerio.deploy.systemcore.RobotCommandArtifact;
import org.wpilib.gradlerio.deploy.systemcore.RobotProgramKillArtifact;
import org.wpilib.gradlerio.deploy.systemcore.RobotProgramStartArtifact;
import org.wpilib.gradlerio.deploy.systemcore.SystemCore;
import org.wpilib.deployutils.deploy.NamedObjectFactory;

public class FRCDeployPlugin implements Plugin<Project> {

    private Project project;

    public static final String LIB_DEPLOY_DIR = "/home/systemcore/frc/third-party/lib";

    private void configureSystemCoreTypes(SystemCore target) {
        ObjectFactory objects = target.getProject().getObjects();
        ExtensiblePolymorphicDomainObjectContainer<DeployLocation> locations = target.getLocations();
        ExtensiblePolymorphicDomainObjectContainer<Artifact> artifacts = target.getArtifacts();

        NamedObjectFactory.registerType(FRCJavaArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(FRCNativeArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(RobotProgramKillArtifact.class, artifacts, target, objects);
        NamedObjectFactory.registerType(RobotProgramStartArtifact.class, artifacts, target, objects);
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
