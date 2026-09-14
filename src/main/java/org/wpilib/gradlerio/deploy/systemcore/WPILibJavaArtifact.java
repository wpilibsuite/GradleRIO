package org.wpilib.gradlerio.deploy.systemcore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.internal.JavaPluginHelper;
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.bundling.Jar;
import org.wpilib.deployutils.PathUtils;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.gradlerio.deploy.DebuggableJavaArtifact;
import org.wpilib.gradlerio.deploy.DeployStage;
import org.wpilib.gradlerio.deploy.WPILibDeployPlugin;
import org.wpilib.gradlerio.wpi.WPIExtension;

public class WPILibJavaArtifact extends DebuggableJavaArtifact {
    public static final String CLASSPATH_PATH = "/home/systemcore/wpilib/classpath";

    private final RobotCommandArtifact robotCommandArtifact;

    private final WPILibJNILibraryArtifact nativeZipArtifact;

    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();

    private final SystemCore systemCore;

    private final Property<String> mainClass;

    private GarbageCollectorType gcType = GarbageCollectorType.ZGC;

    private String javaCommand = "/usr/bin/java";

    private final Property<Boolean> debugJni;

    public Property<Boolean> getDebugJni() {
        return debugJni;
    }

    @Inject
    public WPILibJavaArtifact(String name, SystemCore target) {
        super(name, target);
        systemCore = target;
        debugJni = target.getProject().getObjects().property(Boolean.class);
        debugJni.set(false);

        jvmArgs.add("-Djava.library.path=" + WPILibDeployPlugin.LIB_DEPLOY_DIR);
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/jdk.internal.vm=ALL-UNNAMED");
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/java.lang=ALL-UNNAMED");
        jvmArgs.add("--enable-native-access=ALL-UNNAMED");

        var debugConfiguration = target.getProject().getConfigurations().create("systemcoreDebug");
        var releaseConfiguration = target.getProject().getConfigurations().create("systemcoreRelease");

        this.mainClass = target.getProject().getObjects().property(String.class);

        this.getDirectory().set(CLASSPATH_PATH);
        this.getDeleteOldFiles().set(true);

        robotCommandArtifact = target.getArtifacts().create("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setRobotCommandFunc(this::generateStartCommand);
            art.setArgFileFunc(this::generateArgFile);
            art.dependsOn(getJarProvider());
            art.dependsOn(getConfigurationProvider());
            art.dependsOn(this.getDeployTask());
        });

        nativeZipArtifact = target.getArtifacts().create("nativeZips" + name, WPILibJNILibraryArtifact.class, artifact -> {
            target.setDeployStage(artifact, DeployStage.FileDeploy);

            var cbl = target.getProject().getProviders().provider(() -> {
                boolean debug = getDebugJni().get();
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

    public void configureApplication(JavaApplication javaApplication) {
        JvmFeatureInternal mainFeature = JavaPluginHelper.getJavaComponent(getTarget().getProject()).getMainFeature();
        setConfiguration(mainFeature.getRuntimeClasspathConfiguration());
        setJar(mainFeature.getJarTask().get());
        this.mainClass.set(javaApplication.getMainClass());
    }

    public RobotCommandArtifact getRobotCommandArtifact() {
        return robotCommandArtifact;
    }

    public WPILibJNILibraryArtifact getNativeZipArtifact() {
        return nativeZipArtifact;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public List<String> getArguments() {
        return arguments;
    }

    private String generateArgFile(DeployContext ctx) {
        List<String> args = new ArrayList<>();
        args.addAll(gcType.getGcArguments());
        args.addAll(jvmArgs);

        args.add("-cp \"\\");

        // Put the entire deploy classpath
        String deployDirectory = getDirectory().get();

        List<File> files = new ArrayList<>(getFiles().get().getFiles());

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String path = PathUtils.combine(deployDirectory, file.getName());
            if (i != files.size() - 1) {
                args.add(path + ":\\");
            } else {
                args.add(path + "\"");
            }
        }

        // Debug stuff
        boolean debug = systemCore.getDebug().get();
        if (debug) {
            args.add("-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=0.0.0.0:" + getDebugPort() + ",server=y,suspend=y");
        }

        args.add(mainClass.get());

        args.addAll(arguments);

        args.add("");

        return String.join("\n", args);
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();

        builder.append(javaCommand);

        builder.append(" @");
        builder.append(PathUtils.combine(ctx.getWorkingDir(), RobotCommandArtifact.ARG_FILE));

        return builder.toString();
    }
}
