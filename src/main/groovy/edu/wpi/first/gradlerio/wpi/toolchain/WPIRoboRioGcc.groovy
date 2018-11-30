package edu.wpi.first.gradlerio.wpi.toolchain

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.toolchain.install.ToolchainInstallTask
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
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
    ETLogger logger

    WPIRoboRioGcc(Project project,
                  Instantiator instantiator, String name, BuildOperationExecutor buildOperationExecutor,
                  OperatingSystem operatingSystem, FileResolver fileResolver,
                  ExecActionFactory execActionFactory, CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory,
                  CompilerMetaDataProviderFactory metaDataProviderFactory, WorkerLeaseService workerLeaseService,
                  SystemLibraryDiscovery systemLibraryDiscovery) {

        super(name, buildOperationExecutor, operatingSystem,
                fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory,
                metaDataProviderFactory.gcc(), systemLibraryDiscovery, instantiator, workerLeaseService)

        logger = ETLoggerFactory.INSTANCE.create(this.class.simpleName)

        WPIToolchainPlugin plugin = project.plugins.getPlugin(WPIToolchainPlugin)
        ToolchainDiscoverer toolchainDiscoverer = plugin.maybeDiscoverRoborioToolchain()
        if (toolchainDiscoverer == null) {
            logger.info(plugin.explainToolchains())
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                def isCurrentlyInstalling = graph.allTasks.find { Task t -> t instanceof ToolchainInstallTask } != null
                if (!isCurrentlyInstalling) {
                    def failBuild = !project.hasProperty("toolchain-missing-nofail")

                    logger.logError("===============================================================")
                    logger.logErrorHead("No RoboRIO Toolchain Found! Run `./gradlew installToolchain` to install one!")
                    logger.logErrorHead("  ")
                    if (failBuild)
                        logger.logErrorHead("You can ignore this error with -Ptoolchain-missing-nofail.")
                    else
                        logger.logErrorHead("Native C/C++ RoboRIO builds are being SKIPPED!")
                    logger.logErrorHead("Run with `--info` for more information.")
                    logger.logError("===============================================================")

                    if (failBuild)
                        throw new WPIToolchainPlugin.ToolchainNotFoundException(WPIToolchainPlugin.notoolchainMessage)
                }
            }

            setTargets(WPIExtension.Platforms.roborio)
            path("NOTOOLCHAINPATH")
        } else {
            logger.info("Using Toolchain: " + toolchainDiscoverer.name)

            // On Linux, we can't specify the install location of the apt-get package, so instead we
            // assume it's on the system path
            boolean customPath = toolchainDiscoverer.sysroot().isPresent()

            def frcYear = project.extensions.getByType(WPIExtension).frcYear

            setTargets(WPIExtension.Platforms.roborio)
            eachPlatform(new Action<GccPlatformToolChain>() {
                @Override
                void execute(GccPlatformToolChain target) {
                    target.cCompiler.executable =           toolchainDiscoverer.composeTool("gcc", frcYear)
                    target.cppCompiler.executable =         toolchainDiscoverer.composeTool("g++", frcYear)
                    target.linker.executable =              toolchainDiscoverer.composeTool("g++", frcYear)
                    target.assembler.executable =           toolchainDiscoverer.composeTool("as", frcYear)
                    target.staticLibArchiver.executable =   toolchainDiscoverer.composeTool("ar", frcYear)
                }
            })

            if (customPath)
                path(toolchainDiscoverer.binDir().get())
        }
    }

    @Override
    public String getTypeName() {
        return "RoboRioGcc"
    }
}
