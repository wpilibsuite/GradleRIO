package org.wpilib.gradlerio.deploy.systemcore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import org.wpilib.deployutils.PathUtils;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.gradlerio.deploy.DebuggableJavaArtifact;
import org.wpilib.gradlerio.deploy.DeployStage;
import org.wpilib.gradlerio.deploy.FRCDeployPlugin;
import org.wpilib.gradlerio.wpi.WPIExtension;

public class FRCJavaArtifact extends DebuggableJavaArtifact {

    private final RobotCommandArtifact robotCommandArtifact;

    private final FRCJNILibraryArtifact nativeZipArtifact;

    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();

    private final SystemCore systemCore;

    private GarbageCollectorType gcType = GarbageCollectorType.G1_Base;

    private String javaCommand = "/usr/bin/java";

    @Inject
    public FRCJavaArtifact(String name, SystemCore target) {
        super(name, target);
        systemCore = target;

        jvmArgs.add("-Djava.library.path=" + FRCDeployPlugin.LIB_DEPLOY_DIR);

        var debugConfiguration = target.getProject().getConfigurations().create("systemcoreDebug");
        var releaseConfiguration = target.getProject().getConfigurations().create("systemcoreRelease");

        robotCommandArtifact = target.getArtifacts().create("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setStartCommandFunc(this::generateStartCommand);
            art.dependsOn(getJarProvider());
        });

        nativeZipArtifact = target.getArtifacts().create("nativeZips" + name, FRCJNILibraryArtifact.class, artifact -> {
            target.setDeployStage(artifact, DeployStage.FileDeploy);

            var cbl = target.getProject().getProviders().provider(() -> {
                boolean debug = target.getProject().getExtensions().getByType(WPIExtension.class).getJava().getDebugJni().get();
                if (debug) {
                    return debugConfiguration;
                } else {
                    return releaseConfiguration;
                }
            });

            artifact.getConfiguration().set(cbl);
            artifact.setZipped(true);
            artifact.getFilter().include("**/*.so*");
            artifact.getFilter().include("**/*.so");
            artifact.getFilter().getExcludes().add("**/*.so.debug");
            artifact.getFilter().getExcludes().add("**/*.so.*.debug");
        });

        target.setDeployStage(this, DeployStage.FileDeploy);
    }

    public String getJavaCommand() {
        return javaCommand;
    }

    public void setJavaCommand(String javaCommand) {
        this.javaCommand = javaCommand;
    }

    public GarbageCollectorType getGcType() {
        return gcType;
    }

    public void setGcType(GarbageCollectorType gcType) {
        this.gcType = gcType;
    }

    private String getBinFile(DeployContext ctx) {
        return PathUtils.combine(ctx.getWorkingDir(), getFilename().getOrElse(getFile().get().getName()));
    }

    @Override
    public void setJarTask(Jar jarTask) {
        robotCommandArtifact.getDeployTask().configure(x -> x.dependsOn(jarTask));
        super.setJarTask(jarTask);
    }

    @Override
    public void setJarTask(TaskProvider<Jar> jarTask) {
        robotCommandArtifact.getDeployTask().configure(x -> x.dependsOn(jarTask));
        super.setJarTask(jarTask);
    }

    public RobotCommandArtifact getRobotCommandArtifact() {
        return robotCommandArtifact;
    }

    public FRCJNILibraryArtifact getNativeZipArtifact() {
        return nativeZipArtifact;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public List<String> getArguments() {
        return arguments;
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append(javaCommand);
        builder.append(" ");
        builder.append(String.join(" ", gcType.getGcArguments()));
        builder.append(" ");
        builder.append(String.join(" ", jvmArgs));
        builder.append(" ");

        // Debug stuff
        boolean debug = systemCore.getDebug().get();
        if (debug) {
            builder.append("-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=0.0.0.0:");
            builder.append(getDebugPort());
            builder.append(",server=y,suspend=y ");
        }

        String binFile = getBinFile(ctx);

        builder.append("-jar \"");
        builder.append(binFile);
        builder.append("\" ");
        builder.append(String.join(" ", arguments));

        return builder.toString();
    }
}
