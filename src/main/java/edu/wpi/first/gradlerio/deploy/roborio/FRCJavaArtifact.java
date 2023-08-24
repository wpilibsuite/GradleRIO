package edu.wpi.first.gradlerio.deploy.roborio;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DebuggableJavaArtifact;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.FRCDeployPlugin;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class FRCJavaArtifact extends DebuggableJavaArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final FRCJREArtifact jreArtifact;
    private final FRCJNILibraryArtifact nativeZipArtifact;

    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();

    private final RoboRIO roboRIO;

    private GarbageCollectorType gcType = GarbageCollectorType.Serial;

    private String javaCommand = "/usr/local/frc/JRE/bin/java";

    @Inject
    public FRCJavaArtifact(String name, RoboRIO target) {
        super(name, target);
        roboRIO = target;

        jvmArgs.add("-XX:+AlwaysPreTouch");
        jvmArgs.add("-Djava.lang.invoke.stringConcat=BC_SB");
        jvmArgs.add("-Djava.library.path=" + FRCDeployPlugin.LIB_DEPLOY_DIR);

        var debugConfiguration = target.getProject().getConfigurations().create("roborioDebug");
        var releaseConfiguration = target.getProject().getConfigurations().create("roborioRelease");

        programStartArtifact = target.getArtifacts().create("programStart" + name, FRCProgramStartArtifact.class, art -> {
        });

        jreArtifact = target.getArtifacts().create("jre" + name, FRCJREArtifact.class, art -> {
        });

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

        programStartArtifact.getPostdeploy().add(this::postStart);

        getPostdeploy().add(ctx -> {
            String binFile = getBinFile(ctx);
            ctx.execute("chmod +x \"" + binFile + "\"; chown lvuser \"" + binFile + "\"");
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

    public FRCJREArtifact getJreArtifact() {
        return jreArtifact;
    }

    public FRCProgramStartArtifact getProgramStartArtifact() {
        return programStartArtifact;
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
        boolean debug = roboRIO.getDebug().get();
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

    private void postStart(DeployContext ctx) {
        boolean debug = roboRIO.getDebug().get();
        if (debug) {
            ctx.getLogger().withLock(x -> {
                x.log("====================================================================");
                x.log("DEBUGGING ACTIVE ON PORT " + getDebugPort() + "!");
                x.log("====================================================================");
            });
        }
    }

}
