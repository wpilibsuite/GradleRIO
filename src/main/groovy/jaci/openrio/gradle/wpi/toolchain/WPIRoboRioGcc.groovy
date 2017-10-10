package jaci.openrio.gradle.wpi.toolchain

import jaci.gradle.toolchains.CrossGcc
import jaci.openrio.gradle.wpi.toolchain.install.AbstractToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.LinuxToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.MacOSToolchainInstaller
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

        AbstractToolchainInstaller activeInstaller = WPIToolchainPlugin.getActiveInstaller()
        // On Linux, we can't specify the install location of the apt-get package, so instead we
        // assume it's on the system path
        boolean customPath = !(activeInstaller instanceof LinuxToolchainInstaller)

        target('roborio', new Action<GccPlatformToolChain>() {
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


                if (customPath) {
                    // Sysroot is usually /frc, but since we're overriding the default install directory,
                    // we can modify the sysroot in order to support the location. This is the base for system libs
                    // and such.
                    def sysroot = WPIToolchainPlugin.toolchainInstallDirectory().absolutePath

                    // For some reason mac buries required libs one level deeper than windows
                    if (activeInstaller instanceof MacOSToolchainInstaller)
                        sysroot = new File(sysroot, "arm-frc-linux-gnueabi").absolutePath
                    target.cCompiler.withArguments { a ->
                        a << '--sysroot' << sysroot
                    }
                    target.cppCompiler.withArguments { a ->
                        a << '--sysroot' << sysroot
                    }
                    target.linker.withArguments { a ->
                        a << '--sysroot' << sysroot
                    }
                    target.assembler.withArguments { a ->
                        a << '--sysroot' << sysroot
                    }
                    target.staticLibArchiver.withArguments { a ->
                        a << '--sysroot' << sysroot
                    }
                }

                target.cppCompiler.withArguments { a ->
                    a << '-std=c++1y'
                }
            }
        })

        if (customPath)
            path(new File(WPIToolchainPlugin.toolchainInstallDirectory(), 'bin'))
    }

    @Override
    public String getTypeName() {
        return "RoboRioGcc"
    }
}
