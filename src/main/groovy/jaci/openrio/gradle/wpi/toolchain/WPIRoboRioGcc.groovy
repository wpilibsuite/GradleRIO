package jaci.openrio.gradle.wpi.toolchain

import groovy.transform.CompileStatic
import jaci.openrio.gradle.wpi.toolchain.discover.AbstractToolchainDiscoverer
import jaci.openrio.gradle.wpi.toolchain.install.AbstractToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.LinuxToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.MacOSToolchainInstaller
import org.apache.log4j.Logger
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory
import org.gradle.process.internal.ExecActionFactory

@CompileStatic
class WPIRoboRioGcc extends AbstractGccCompatibleToolChain {
    WPIRoboRioGcc(Project project,
                  Instantiator instantiator, String name, BuildOperationExecutor buildOperationExecutor,
                  OperatingSystem operatingSystem, FileResolver fileResolver,
                  ExecActionFactory execActionFactory, CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory,
                  CompilerMetaDataProviderFactory metaDataProviderFactory, WorkerLeaseService workerLeaseService,
                  SystemLibraryDiscovery systemLibraryDiscovery) {

        super(name, buildOperationExecutor, operatingSystem,
                fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory,
                metaDataProviderFactory.gcc(), systemLibraryDiscovery, instantiator, workerLeaseService)

        AbstractToolchainInstaller activeInstaller = WPIToolchainPlugin.getActiveInstaller()
        AbstractToolchainDiscoverer toolchainDiscoverer = project.plugins.getPlugin(WPIToolchainPlugin).discoverRoborioToolchain()
        Logger.getLogger(this.class).info("Using Toolchain: " + toolchainDiscoverer.class.name)
        // On Linux, we can't specify the install location of the apt-get package, so instead we
        // assume it's on the system path
        boolean customPath = toolchainDiscoverer.sysroot().isPresent()

        setTargets('roborio')
        eachPlatform(new Action<GccPlatformToolChain>() {
            @Override
            void execute(GccPlatformToolChain target) {
                target.cCompiler.executable =           toolchainDiscoverer.composeTool("gcc")
                target.cppCompiler.executable =         toolchainDiscoverer.composeTool("g++")
                target.linker.executable =              toolchainDiscoverer.composeTool("g++")
                target.assembler.executable =           toolchainDiscoverer.composeTool("as")
                target.staticLibArchiver.executable =   toolchainDiscoverer.composeTool("ar")


                if (customPath) {
                    // Sysroot is usually /frc, but since we're overriding the default install directory,
                    // we can modify the sysroot in order to support the location. This is the base for system libs
                    // and such.
                    def sysroot = toolchainDiscoverer.sysroot().get().absolutePath

                    target.cCompiler.withArguments ({ List<String> a ->
                        a << '--sysroot' << sysroot
                    } as Action<? super List<String>>)

                    target.cppCompiler.withArguments ({ List<String> a ->
                        a << '--sysroot' << sysroot
                    } as Action<? super List<String>>)

                    target.linker.withArguments ({ List<String> a ->
                        a << '--sysroot' << sysroot
                    } as Action<? super List<String>>)
                }

                target.cppCompiler.withArguments ({ List<String> a ->
                    a << '-pthread'
                } as Action<? super List<String>>)

                target.linker.withArguments ({ List<String> a ->
                    a << '-pthread' << '-rdynamic'
                } as Action<? super List<String>>)
            }
        })

        if (customPath)
            path(toolchainDiscoverer.binDir().get())
    }

    @Override
    public String getTypeName() {
        return "RoboRioGcc"
    }
}
