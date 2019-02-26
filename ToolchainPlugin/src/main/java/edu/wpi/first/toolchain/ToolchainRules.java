package edu.wpi.first.toolchain;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.work.WorkerLeaseService;
import org.gradle.model.Defaults;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.SharedLibraryBinarySpec;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery;
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.BinaryTasks;
import org.gradle.platform.base.PlatformContainer;
import org.gradle.process.internal.ExecActionFactory;

import jaci.gradle.log.ETLogger;
import jaci.gradle.log.ETLoggerFactory;

public class ToolchainRules extends RuleSource {

    private static final ETLogger logger = ETLoggerFactory.INSTANCE.create("ToolchainRules");

    @Defaults
    void addDefaultToolchains(NativeToolChainRegistryInternal toolChainRegistry, ServiceRegistry serviceRegistry, ExtensionContainer extContainer) {
        final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
        final ExecActionFactory execActionFactory = serviceRegistry.get(ExecActionFactory.class);
        final CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory = serviceRegistry.get(CompilerOutputFileNamingSchemeFactory.class);
        final Instantiator instantiator = serviceRegistry.get(Instantiator.class);
        final BuildOperationExecutor buildOperationExecutor = serviceRegistry.get(BuildOperationExecutor.class);
        final CompilerMetaDataProviderFactory metaDataProviderFactory = serviceRegistry.get(CompilerMetaDataProviderFactory.class);
        final WorkerLeaseService workerLeaseService = serviceRegistry.get(WorkerLeaseService.class);
        final SystemLibraryDiscovery standardLibraryDiscovery = serviceRegistry.get(SystemLibraryDiscovery.class);

        final ToolchainExtension ext = extContainer.getByType(ToolchainExtension.class);

        ext.all(desc -> {
            logger.info("Descriptor Register: " + desc.getName());
            ToolchainOptions options = new ToolchainOptions(instantiator, buildOperationExecutor, OperatingSystem.current(),
                            fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory, metaDataProviderFactory, workerLeaseService,
                            standardLibraryDiscovery);
            options.descriptor = desc;

            desc.getRegistrar().register(options, toolChainRegistry, instantiator);
        });
    }

    @Mutate
    void addDefaultPlatforms(final ExtensionContainer extContainer, final PlatformContainer platforms) {
        final ToolchainExtension ext = extContainer.getByType(ToolchainExtension.class);

        if (ext.registerPlatforms) {
            NativePlatform roborio = platforms.maybeCreate(NativePlatforms.roborio, NativePlatform.class);
            roborio.architecture("arm");
            roborio.operatingSystem("linux");

            NativePlatform raspbian = platforms.maybeCreate(NativePlatforms.raspbian, NativePlatform.class);
            raspbian.architecture("arm");
            raspbian.operatingSystem("linux");

            NativePlatform desktop = platforms.maybeCreate(NativePlatforms.desktop, NativePlatform.class);
            desktop.architecture(NativePlatforms.desktopArch().replaceAll("-", "_"));
        }
    }

    @Validate
    void checkEnabledToolchains(final BinaryContainer binaries, final NativeToolChainRegistry toolChains) {
        // Map of platform to toolchains
        Map<String, GccToolChain> gccToolChains = new HashMap<>();
        for(NativeToolChain toolChain : toolChains) {
            if (toolChain instanceof GccToolChain) {
                GccToolChain gccToolChain = (GccToolChain)toolChain;
                for (String name : gccToolChain.getDescriptor().getToolchainPlatforms()) {
                    gccToolChains.put(name, gccToolChain);
                }
                
            }
        }

        for (BinarySpec oBinary : binaries) {
            if (!(oBinary instanceof NativeBinarySpec)) {
                break;
            }
            NativeBinarySpec binary = (NativeBinarySpec)oBinary;
            GccToolChain chain = gccToolChains.getOrDefault(binary.getTargetPlatform().getName(), null);
            // Can't use getToolChain, as that is invalid for unknown platforms
            if (chain != null) {
                chain.setUsed(true);
            }
        }
    }

    @BinaryTasks
    void createNativeStripTasks(final ModelMap<Task> tasks, final ExtensionContainer extContainer, final NativeBinarySpec binary) {
        final Project project = extContainer.getByType(ToolchainPlugin.ProjectWrapper.class).getProject();
        GccToolChain gccTmp = null;
        if (binary.getToolChain() instanceof GccToolChain) {
            gccTmp = (GccToolChain)binary.getToolChain();
        } else {
            return;
        }
        GccToolChain gcc = gccTmp;
        Task rawLinkTask = null;
        if (binary instanceof SharedLibraryBinarySpec) {
            rawLinkTask = ((SharedLibraryBinarySpec)binary).getTasks().getLink();
        } else if (binary instanceof NativeExecutableBinarySpec) {
            rawLinkTask = ((NativeExecutableBinarySpec)binary).getTasks().getLink();
        }
        if (!(rawLinkTask instanceof AbstractLinkTask)) {
            return;
        }
        AbstractLinkTask linkTask = (AbstractLinkTask)rawLinkTask;

        linkTask.doLast((it) -> {
            File mainFile = linkTask.getLinkedFile().get().getAsFile();

            if (mainFile.exists()) {
                String mainFileStr = mainFile.toString();
                String debugFile = mainFileStr + ".debug";

                ToolchainDiscoverer disc = gcc.getDiscoverer();

                Optional<File> objcopyOptional = disc.tool("objcopy");
                Optional<File>  stripOptional = disc.tool("strip");
                if (!objcopyOptional.isPresent() || !stripOptional.isPresent()) {
                    ETLogger logger = ETLoggerFactory.INSTANCE.create("NativeBinaryStrip");
                    logger.logError("Failed to strip binaries because of unknown tool objcopy and strip");
                    return;
                }

                String objcopy = disc.tool("objcopy").get().toString();
                String strip = disc.tool("strip").get().toString();

                project.exec((ex) -> {
                    ex.commandLine(objcopy, "--only-keep-debug", mainFileStr, debugFile);
                });
                project.exec((ex) -> {
                    ex.commandLine(strip, "-g", mainFileStr);
                });
                project.exec((ex) -> {
                    ex.commandLine(objcopy, "--add-gnu-debuglink=" + debugFile, mainFileStr);
                });
            }
        });
    }
}
