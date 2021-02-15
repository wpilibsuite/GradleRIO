package edu.wpi.first.gradlerio.deploy;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import edu.wpi.first.deployutils.PathUtils;
import edu.wpi.first.deployutils.deploy.DeployExtension;
import edu.wpi.first.deployutils.deploy.DeployPlugin.ArtifactBinaryLinkTaskTuple;
import edu.wpi.first.deployutils.deploy.artifact.BinaryLibraryArtifact;
import edu.wpi.first.deployutils.deploy.artifact.NativeArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;

public class FRCNativeArtifact extends NativeArtifact {

    private final FRCProgramStartArtifact programStartArtifact;
    private final RobotCommandArtifact robotCommandArtifact;
    private final List<String> arguments = new ArrayList<>();
    private boolean debug = false;
    private int debugPort = 8348;
    private Action<BinaryLibraryArtifact> onLocalBlaCreated;

    @Inject
    public FRCNativeArtifact(String name, Project project) {
        super(name, project);

        super.setOnBinaryLibraryArtifactCreated(bla -> {
            bla.getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
            bla.getPostdeploy().add(ctx -> {
                FRCPlugin.ownDirectory(ctx, FRCPlugin.LIB_DEPLOY_DIR);
                ctx.execute("ldconfig");
            });
            if (onLocalBlaCreated != null) {
                onLocalBlaCreated.execute(bla);
            }
        });

        DeployExtension de = project.getExtensions().getByType(DeployExtension.class);

        programStartArtifact = de.getArtifacts().artifact("programStart" + name, FRCProgramStartArtifact.class, art -> {
        });

        robotCommandArtifact = de.getArtifacts().artifact("robotCommand" + name, RobotCommandArtifact.class, art -> {
            art.setStartCommandFunc(this::generateStartCommand);
        });

        programStartArtifact.getPostdeploy().add(this::postStart);

        getPostdeploy().add(ctx -> {
            String binFile = PathUtils.combine(ctx.getWorkingDir(),
                    getFilename() != null ? getFilename() : getFile().get().getName());
            ctx.execute("chmod +x \"" + binFile + "\"; chown lvuser \"" + binFile + "\"");
            // Let user program set RT thread priorities by making CAP_SYS_NICE
            // permitted, inheritable, and effective. See "man 7 capabilities"
            // for docs on capabilities and file capability sets.
            ctx.execute("setcap cap_sys_nice+eip \"" + binFile + "\"");
        });

        setBuildType("<<GR_AUTO>>");
        this.getLibraryDirectory().set(FRCPlugin.LIB_DEPLOY_DIR);

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    @Override
    public void setTarget(Object tObj) {
        programStartArtifact.setTarget(tObj);
        robotCommandArtifact.setTarget(tObj);
        super.setTarget(tObj);
    }

    @Override
    public Action<BinaryLibraryArtifact> getOnBinaryLibraryArtifactCreated() {
        return onLocalBlaCreated;
    }

    @Override
    public void setOnBinaryLibraryArtifactCreated(Action<BinaryLibraryArtifact> onBinaryLibraryArtifactCreated) {
        this.onLocalBlaCreated = onBinaryLibraryArtifactCreated;
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

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public String getBuildType() {
        String sup = super.getBuildType();
        if (!sup.equals("<<GR_AUTO>>")) {
            return sup;
        }
        return debug ? "debug" : "release";
    }

    @Override
    public void configureBlaArtifact(ArtifactBinaryLinkTaskTuple toAdd, DeployExtension de) {
        if (this != toAdd.getArtifact()) {
            throw new GradleException("Can only configure this target");
        }
        robotCommandArtifact.getDeployTask().configure(x -> x.dependsOn(toAdd.getLinkTask()));
        super.configureBlaArtifact(toAdd, de);
    }

    private String generateStartCommand(DeployContext ctx) {
        StringBuilder builder = new StringBuilder();
        if (debug) {
            builder.append("gdbserver host:");
            builder.append(debugPort);
            builder.append(' ');
        }
        builder.append('\"');
        String binFile = PathUtils.combine(ctx.getWorkingDir(),
                getFilename() != null ? getFilename() : getFile().get().getName());
        builder.append(binFile);
        builder.append("\" ");
        builder.append(String.join(" ", arguments));

        return builder.toString();
    }

    private void postStart(DeployContext ctx) {

    }
}
