package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.sessions.IPSessionController;
import edu.wpi.first.gradlerio.deploy.DebuggableJavaArtifact;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.FRCPlugin;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class FRCJavaArtifact extends DebuggableJavaArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final FRCJREArtifact jreArtifact;
    private final FRCJNILibraryArtifact nativeZipArtifact;

    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();

    private final RoboRIO roboRIO;

    @Inject
    public FRCJavaArtifact(String name, RoboRIO target) {
        super(name, target);
        roboRIO = target;

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
            artifact.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);

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

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
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
        builder.append("/usr/local/frc/JRE/bin/java -XX:+UseConcMarkSweepGC -Djava.library.path=");
        builder.append(FRCPlugin.LIB_DEPLOY_DIR);
        builder.append(" -Djava.lang.invoke.stringConcat=BC_SB ");
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
        // File conffile = new File(getTarget().getProject().getBuildDir(),
        //         "debug/" + getName() + "_" + ctx.getDeployLocation().getTarget().getName() + ".debugconfig");

        // boolean debug = roboRIO.getDebug().get();
        // if (debug) {
        //     conffile.getParentFile().mkdirs();

        //     ctx.getLogger().withLock(x -> {
        //         x.log("====================================================================");
        //         x.log("DEBUGGING ACTIVE ON PORT " + getDebugPort() + "!");
        //         x.log("====================================================================");
        //     });

        //     if (ctx.getController() instanceof IPSessionController) {
        //         IPSessionController ip = (IPSessionController) ctx.getController();
        //         String target = ip.getHost() + ":" + getDebugPort();
        //         Map<String, Object> dbcfg = Map.of("target", target, "ipAddress", ip.getHost(), "port", getDebugPort());
        //         GsonBuilder builder = new GsonBuilder();
        //         builder.setPrettyPrinting();
        //         try {
        //             ResourceGroovyMethods.setText(conffile, builder.create().toJson(dbcfg));
        //         } catch (IOException e) {
        //             throw new RuntimeException(e);
        //         }
        //     } else {
        //         ctx.getLogger().log("Session Controller isn't IP Compatible. No debug file written");
        //     }
        // } else {
        //     if (conffile.exists()) conffile.delete();
        // }
    }

}
