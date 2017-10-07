package jaci.openrio.gradle.wpi.toolchain

import jaci.gradle.toolchains.CrossGcc
import org.gradle.api.Action
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory
import org.gradle.process.internal.ExecActionFactory

class WPIRoboRioGcc extends CrossGcc {
    WPIRoboRioGcc(Instantiator instantiator, String name, BuildOperationExecutor buildOperationExecutor,
                  OperatingSystem operatingSystem, FileResolver fileResolver,
                  ExecActionFactory execActionFactory, CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory,
                  CompilerMetaDataProviderFactory metaDataProviderFactory, WorkerLeaseService workerLeaseService) {

        super(instantiator, name, buildOperationExecutor, operatingSystem,
                fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory,
                metaDataProviderFactory, workerLeaseService)

        target("roborio", new Action<GccPlatformToolChain>() {
            @Override
            void execute(GccPlatformToolChain target) {
                String gccPrefix = "arm-frc-linux-gnueabi-"
                String gccSuffix = OperatingSystem.current().isWindows() ? ".exe" : ""

                target.cCompiler.executable =           gccPrefix + "gcc" + gccSuffix
                target.cCompiler.withArguments      { a -> a << "-DROBORIO" }   // Define the 'ROBORIO' macro
                target.cppCompiler.executable =         gccPrefix + "g++" + gccSuffix
                target.cppCompiler.withArguments    { a -> a << "-DROBORIO" }   // Define the 'ROBORIO' macro
                target.linker.executable =              gccPrefix + "g++" + gccSuffix
                target.assembler.executable =           gccPrefix + "as"  + gccSuffix
                target.staticLibArchiver.executable =   gccPrefix + "ar"  + gccSuffix
            }
        })
        
        path(WPIToolchainPlugin.toolchainInstallDirectory())
    }

    @Override
    public String getTypeName() {
        return "RoboRioGcc"
    }
}
