package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.artifact.JavaArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.sessions.IPSessionController;

public class FRCJavaArtifact extends JavaArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final FRCJREArtifact jreArtifact;
    private final FRCJNILibraryArtifact nativeLibArtifact;
    private final FRCJNILibraryArtifact nativeZipArtifact;

    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();
    private boolean debug = false;
    private int debugPort = 8349;

    @Inject
    public FRCJavaArtifact(String name, Project project) {
        super(name, project);

        DeployExtension de = project.getExtensions().getByType(DeployExtension.class);

        programStartArtifact = de.getArtifacts().artifact("programStart" + name, FRCProgramStartArtifact.class, art -> {
        });

        jreArtifact = de.getArtifacts().artifact("jre" + name, FRCJREArtifact.class, art -> {
        });

        robotCommandArtifact = de.getArtifacts().artifact("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setStartCommandFunc(this::generateStartCommand);
        });


        Configuration nativeLibs = project.getConfigurations().getByName("nativeLib");
        Configuration nativeZips = project.getConfigurations().getByName("nativeZip");

        nativeLibArtifact = de.getArtifacts().artifact("nativeLibs" + name, FRCJNILibraryArtifact.class, artifact -> {
            artifact.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
            artifact.setConfiguration(nativeLibs);
            artifact.setZipped(false);
        });

        nativeZipArtifact = de.getArtifacts().artifact("nativeZips" + name, FRCJNILibraryArtifact.class, artifact -> {
            artifact.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
            artifact.setConfiguration(nativeZips);
            artifact.setZipped(true);
            artifact.setFilter(pat -> {
                pat.include("*.so*", "lib/*.so", "java/lib/*.so", "linux/athena/shared/*.so", "linuxathena/**/*.so", "**/libopencv*.so.*");
            });
        });

        programStartArtifact.getPostdeploy().add(this::postStart);

        getPostdeploy().add(ctx -> {
            String binFile = PathUtils.combine(ctx.getWorkingDir(),
                    getFilename() != null ? getFilename() : getFile().get().getName());
            ctx.execute("chmod +x \"" + binFile + "\"; chown lvuser \"" + binFile + "\"");
        });

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    @Override
    public void setTarget(Object tObj) {
        programStartArtifact.setTarget(tObj);
        robotCommandArtifact.setTarget(tObj);
        jreArtifact.setTarget(tObj);
        nativeLibArtifact.setTarget(tObj);
        nativeZipArtifact.setTarget(tObj);
        super.setTarget(tObj);
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

    public FRCJNILibraryArtifact getNativeLibArtifact() {
        return nativeLibArtifact;
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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append("/usr/local/frc/JRE/bin/java -XX:+UseConcMarkSweepGC -Djava.library.path=");
        builder.append(FRCPlugin.LIB_DEPLOY_DIR);
        builder.append(" -Djava.lang.invoke.stringConcat=BC_SB ");
        builder.append(String.join(" ", jvmArgs));
        builder.append(" ");

        // Debug stuff
        if (debug) {
            builder.append("-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=0.0.0.0:");
            builder.append(debugPort);
            builder.append(",server=y,suspend=y ");
        }

        String binFile = PathUtils.combine(ctx.getWorkingDir(),
                getFilename() != null ? getFilename() : getFile().get().getName());

        builder.append("-jar \"");
        builder.append(binFile);
        builder.append("\" ");
        builder.append(String.join(" ", arguments));

        return builder.toString();
    }

    private void postStart(DeployContext ctx) {
        File conffile = new File(getProject().getBuildDir(),
                "debug/" + getName() + "_" + ctx.getDeployLocation().getTarget().getName() + ".debugconfig");

        if (debug) {
            conffile.getParentFile().mkdirs();

            ctx.getLogger().withLock(x -> {
                x.log("====================================================================");
                x.log("DEBUGGING ACTIVE ON PORT " + debugPort + "!");
                x.log("====================================================================");
            });

            if (ctx.getController() instanceof IPSessionController) {
                IPSessionController ip = (IPSessionController) ctx.getController();
                String target = ip.getHost() + ":" + debugPort;
                Map<String, Object> dbcfg = Map.of("target", target, "ipAddress", ip.getHost(), "port", debugPort);
                GsonBuilder builder = new GsonBuilder();
                builder.setPrettyPrinting();
                try {
                    ResourceGroovyMethods.setText(conffile, builder.create().toJson(dbcfg));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ctx.getLogger().log("Session Controller isn't IP Compatible. No debug file written");
            }
        } else {
            if (conffile.exists()) conffile.delete();
        }
    }

}
