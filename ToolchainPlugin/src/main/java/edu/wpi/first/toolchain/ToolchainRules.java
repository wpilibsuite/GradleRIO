package edu.wpi.first.toolchain;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.work.WorkerLeaseService;
import org.gradle.model.Defaults;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery;
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory;
import org.gradle.platform.base.PlatformContainer;
import org.gradle.process.internal.ExecActionFactory;

public class ToolchainRules extends RuleSource {

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
            ToolchainOptions options = new ToolchainOptions(instantiator, buildOperationExecutor, OperatingSystem.current(),
                            fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory, metaDataProviderFactory, workerLeaseService,
                            standardLibraryDiscovery);
            options.descriptor = desc;

            desc.getRegistrar().register(options, toolChainRegistry, instantiator);
        });
    }

    @Mutate
    void addDefaultPlatforms(PlatformContainer platforms) {
        NativePlatform roborio = platforms.maybeCreate(NativePlatforms.roborio, NativePlatform.class);
        roborio.architecture("arm");
        roborio.operatingSystem("linux");

        NativePlatform desktop = platforms.maybeCreate(NativePlatforms.desktop, NativePlatform.class);
        desktop.architecture(NativePlatforms.desktopArch().replaceAll("-", "_"));
    }

}
