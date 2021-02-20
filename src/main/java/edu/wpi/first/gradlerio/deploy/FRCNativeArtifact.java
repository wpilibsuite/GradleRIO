package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.artifact.NativeExecutableArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public class FRCNativeArtifact extends NativeExecutableArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final List<String> arguments = new ArrayList<>();
    private boolean debug = false;
    private int debugPort = 8348;

    @Inject
    public FRCNativeArtifact(String name, RemoteTarget target) {
        super(name, target);

        getPostdeploy().add(ctx -> {
            FRCPlugin.ownDirectory(ctx, getLibraryDirectory().get());
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

        this.getLibraryDirectory().set(FRCPlugin.LIB_DEPLOY_DIR);

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
        if (debug) {
            builder.append("gdbserver host:");
            builder.append(debugPort);
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

    }
}
