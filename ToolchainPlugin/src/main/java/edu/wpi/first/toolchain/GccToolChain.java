package edu.wpi.first.toolchain;

import jaci.gradle.log.ETLogger;
import jaci.gradle.log.ETLoggerFactory;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.internal.text.TreeFormatter;
import org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain;

public abstract class GccToolChain extends AbstractGccCompatibleToolChain {

    private Project project;
    private ETLogger logger;
    private ToolchainDescriptor descriptor;
    private ToolchainDiscoverer discoverer;
    private boolean isUsed;

    public GccToolChain(ToolchainOptions options) {
        super(options.name,
                options.buildOperationExecutor,
                options.operatingSystem,
                options.fileResolver,
                options.execActionFactory,
                options.compilerOutputFileNamingSchemeFactory,
                options.metaDataProviderFactory.gcc(),
                options.systemLibraryDiscovery,
                options.instantiator,
                options.workerLeaseService);

        this.project = options.project;
        this.descriptor = options.descriptor;
        this.discoverer = descriptor.discover();

        logger = ETLoggerFactory.INSTANCE.create(this.getClass().getSimpleName());

        setTargets(descriptor.getToolchainPlatforms());

        if (discoverer != null) {
            eachPlatform(toolchain -> {
                toolchain.getcCompiler().setExecutable(discoverer.toolName("gcc"));
                toolchain.getCppCompiler().setExecutable(discoverer.toolName("g++"));
                toolchain.getLinker().setExecutable(discoverer.toolName("g++"));
                toolchain.getAssembler().setExecutable(discoverer.toolName("as"));
                toolchain.getStaticLibArchiver().setExecutable(discoverer.toolName("ar"));
            });

            if (discoverer.sysroot().isPresent())
                path(discoverer.binDir().get());
        } else {
            project.getGradle().getTaskGraph().whenReady(graph -> {
                boolean installing = graph.getAllTasks().stream().anyMatch(t -> t instanceof InstallToolchainTask && ((InstallToolchainTask) t).getDescriptorName().equals(descriptor.getName()));
                if (!installing) {
                    TreeFormatter formatter = new TreeFormatter();
                    descriptor.explain(formatter);
                    logger.info(formatter.toString());

                    boolean optional = descriptor.isOptional() || project.hasProperty("toolchain-optional-" + descriptor.getName());
                    if (optional) {
                        logger.logStyle("Skipping builds for " + descriptor.getName() + " (toolchain is marked optional)", StyledTextOutput.Style.Description);
                    } else if (isUsed) {
                        logger.logError("=============================");
                        logger.logErrorHead("No Toolchain Found for " + descriptor.getName());
                        logger.logErrorHead("Run `./gradlew " + descriptor.installTaskName() + "` to install one!");
                        logger.logErrorHead("");
                        logger.logErrorHead("You can ignore this error with -Ptoolchain-optional-" + descriptor.getName());
                        logger.logErrorHead("For more information, run with `--info`");
                        logger.logError("=============================");

                        throw new GradleException("No Toolchain Found! Scroll up for more information.");
                    }
                }
            });

            path("NOTOOLCHAINPATH");
        }
    }

    public Project getProject() {
        return project;
    }

    public ToolchainDescriptor getDescriptor() {
        return descriptor;
    }

    public ToolchainDiscoverer getDiscoverer() {
        return discoverer;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}
