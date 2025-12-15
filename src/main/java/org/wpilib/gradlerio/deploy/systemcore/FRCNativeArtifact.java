package org.wpilib.gradlerio.deploy.systemcore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.NativeExecutableSpec;

import org.wpilib.deployutils.PathUtils;
import org.wpilib.deployutils.deploy.context.DeployContext;
import org.wpilib.gradlerio.deploy.DebuggableNativeArtifact;
import org.wpilib.gradlerio.deploy.DeployStage;
import org.wpilib.gradlerio.deploy.FRCDeployPlugin;

public class FRCNativeArtifact extends DebuggableNativeArtifact {

    private final RobotCommandArtifact robotCommandArtifact;
    private final List<String> arguments = new ArrayList<>();
    private final SystemCore systemCore;

    private final Property<NativeExecutableSpec> componentSpec;

    public Property<NativeExecutableSpec> getComponent() {
        return componentSpec;
    }

    @Inject
    public FRCNativeArtifact(String name, SystemCore target) {
        super(name, target);
        systemCore = target;

        componentSpec = target.getProject().getObjects().property(NativeExecutableSpec.class);

        getBinary().set(componentSpec.map(x -> {
            for (NativeExecutableBinarySpec bin : x.getBinaries().withType(NativeExecutableBinarySpec.class)) {
                if (bin.getTargetPlatform().getName().equals(getTarget().getTargetPlatform().get()) &&
                    bin.getBuildType().getName().equals(target.getBuildType().get())) {
                    return bin;
                }
            }
            return null;
        }));

        PatternFilterable filterable = getLibraryFilter();
        filterable.getExcludes().add("**/*.so.debug");
        filterable.getExcludes().add("**/*.so.*.debug");

        getPostdeploy().add(ctx -> {
            FRCDeployPlugin.ownDirectory(ctx, getLibraryDirectory().get());
            ctx.execute("sudo ldconfig " + FRCDeployPlugin.LIB_DEPLOY_DIR);
        });

        robotCommandArtifact = target.getArtifacts().create("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setStartCommandFunc(this::generateStartCommand);
            art.dependsOn(getInstallTaskProvider());
        });

        getPostdeploy().add(ctx -> {
            String binFile = getBinFile(ctx);
            ctx.execute("chmod +x \"" + binFile + "\"; chown systemcore \"" + binFile + "\"");
            // Let user program set RT thread priorities by making CAP_SYS_NICE
            // permitted, inheritable, and effective. See "man 7 capabilities"
            // for docs on capabilities and file capability sets.
            ctx.execute("sudo setcap cap_sys_nice+eip \"" + binFile + "\"");
        });

        this.getLibraryDirectory().set(FRCDeployPlugin.LIB_DEPLOY_DIR);

        target.setDeployStage(this, DeployStage.FileDeploy);
    }

    private String getBinFile(DeployContext ctx) {
        File exeFile = getDeployedFile();
        return PathUtils.combine(ctx.getWorkingDir(), getFilename().getOrElse(exeFile.getName()));
    }

    public RobotCommandArtifact getRobotCommandArtifact() {
        return robotCommandArtifact;
    }

    public List<String> getArguments() {
        return arguments;
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append("LD_LIBRARY_PATH=\"");
        builder.append(FRCDeployPlugin.LIB_DEPLOY_DIR);
        builder.append("\" ");
        boolean debug = systemCore.getDebug().get();
        if (debug) {
            builder.append("gdbserver host:");
            builder.append(getDebugPort());
            builder.append(' ');
        }
        builder.append('\"');
        String binFile = getBinFile(ctx);
        builder.append(binFile);
        builder.append("\" ");
        builder.append(String.join(" ", arguments));

        return builder.toString();
    }
}
