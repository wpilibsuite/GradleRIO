package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.NativeExecutableSpec;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.deploy.DebuggableNativeArtifact;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.deploy.FRCDeployPlugin;

public class FRCNativeArtifact extends DebuggableNativeArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final List<String> arguments = new ArrayList<>();
    private final RoboRIO roboRIO;

    private final Property<NativeExecutableSpec> componentSpec;

    public Property<NativeExecutableSpec> getComponent() {
        return componentSpec;
    }

    @Inject
    public FRCNativeArtifact(String name, RoboRIO target) {
        super(name, target);
        roboRIO = target;

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
            ctx.execute("ldconfig");
        });

        programStartArtifact = target.getArtifacts().create("programStart" + name, FRCProgramStartArtifact.class, art -> {
        });

        robotCommandArtifact = target.getArtifacts().create("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setStartCommandFunc(this::generateStartCommand);
            art.dependsOn(getInstallTaskProvider());
        });

        programStartArtifact.getPostdeploy().add(this::postStart);

        getPostdeploy().add(ctx -> {
            String binFile = getBinFile(ctx);
            ctx.execute("chmod +x \"" + binFile + "\"; chown lvuser \"" + binFile + "\"");
            // Let user program set RT thread priorities by making CAP_SYS_NICE
            // permitted, inheritable, and effective. See "man 7 capabilities"
            // for docs on capabilities and file capability sets.
            ctx.execute("setcap cap_sys_nice+eip \"" + binFile + "\"");
        });

        this.getLibraryDirectory().set(FRCDeployPlugin.LIB_DEPLOY_DIR);

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    private String getBinFile(DeployContext ctx) {
        File exeFile = getDeployedFile();
        return PathUtils.combine(ctx.getWorkingDir(), getFilename().getOrElse(exeFile.getName()));
    }

    public FRCProgramStartArtifact getProgramStartArtifact() {
        return programStartArtifact;
    }

    public RobotCommandArtifact getRobotCommandArtifact() {
        return robotCommandArtifact;
    }

    public List<String> getArguments() {
        return arguments;
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();
        boolean debug = roboRIO.getDebug().get();
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
