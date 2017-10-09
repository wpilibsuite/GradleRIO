package jaci.openrio.gradle.wpi.toolchain

import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.toolchain.install.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory
import org.gradle.platform.base.PlatformContainer
import org.gradle.process.internal.ExecActionFactory

class WPIToolchainPlugin implements Plugin<Project> {
    static List<AbstractToolchainInstaller> toolchainInstallers = [
        new WindowsToolchainInstaller(),
        new MacOSToolchainInstaller(),
        new LinuxToolchainInstaller()
    ]

    @Override
    void apply(Project project) {
        def rootInstallTask = project.task("installToolchain") { Task task ->
            task.group = "GradleRIO"
            task.description = "Install the C++ FRC Toolchain for this system"

            task.doLast {
                AbstractToolchainInstaller installer = getActiveInstaller()

                if (installer == null) {
                    throw new NoToolchainInstallersException("Cannot install Toolchain! (No Installers for this Platform)")
                } else {
                    installer.installToolchain(task.project)
                }
            }
        }
    }

    public static AbstractToolchainInstaller getActiveInstaller() {
        def toolchains = toolchainInstallers.findAll { t ->
            return t.installable()
        }
        if (toolchains.empty) return null;
        return toolchains.first()
    }

    public static URL toolchainDownloadURL(String file) {
        return new URL("http://first.wpi.edu/FRC/roborio/toolchains/${file}")
    }

    public static File toolchainDownloadDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "cache/toolchains/download")
    }

    public static File toolchainExtractDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "cache/toolchains/extract")
    }

    public static File toolchainInstallDirectory() {
        return new File(GradleRIOPlugin.globalDirectory, "toolchains")
    }

    static class WPIToolchainRules extends RuleSource {
        @Mutate
        void addPlatform(PlatformContainer platforms) {
            def roborio = platforms.maybeCreate('roborio', NativePlatform)
            roborio.architecture('arm')
            roborio.operatingSystem('linux')
        }

        @Mutate
        void addToolchains(NativeToolChainRegistryInternal toolChainRegistry, ServiceRegistry serviceRegistry) {
            def fileResolver = serviceRegistry.get(FileResolver.class)
            def execActionFactory = serviceRegistry.get(ExecActionFactory.class)
            def compilerOutputFileNamingSchemeFactory = serviceRegistry.get(CompilerOutputFileNamingSchemeFactory.class)
            def instantiator = serviceRegistry.get(Instantiator.class)
            def buildOperationExecutor = serviceRegistry.get(BuildOperationExecutor.class)
            def metaDataProviderFactory = serviceRegistry.get(CompilerMetaDataProviderFactory.class)
            def workerLeaseService = serviceRegistry.get(WorkerLeaseService.class)

            toolChainRegistry.registerFactory(WPIRoboRioGcc.class, { String name ->
                return instantiator.newInstance(WPIRoboRioGcc.class, instantiator, name, buildOperationExecutor, OperatingSystem.current(), fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory, metaDataProviderFactory, workerLeaseService)
            })

            toolChainRegistry.registerDefaultToolChain('roborioGcc', WPIRoboRioGcc)
        }
    }
}
