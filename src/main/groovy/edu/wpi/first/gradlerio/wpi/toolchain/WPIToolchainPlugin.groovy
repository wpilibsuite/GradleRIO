package edu.wpi.first.gradlerio.wpi.toolchain

import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.toolchain.install.*
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import jaci.openrio.gradle.wpi.toolchain.install.*
import org.apache.log4j.Logger
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.text.TreeFormatter
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.nativeplatform.toolchain.VisualCpp
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.PlatformContainer
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecActionFactory
import org.gradle.util.TreeVisitor

@CompileStatic
class WPIToolchainPlugin implements Plugin<Project> {

    static final String discovererGradleRIO = "GradleRIO Home"
    static final String notoolchainMessage = "No valid RoboRIO toolchain(s) found! Run `./gradlew installToolchain`, or run with `--info` for more details."

    @CompileStatic
    static class ToolchainNotFoundException extends RuntimeException {
        ToolchainNotFoundException(String msg) {
            super(msg)
        }
    }

    static List<AbstractToolchainInstaller> toolchainInstallers = [
            new WindowsToolchainInstaller(),
            new MacOSToolchainInstaller(),
            new LinuxToolchainInstaller()
    ]

    List<ToolchainDiscoverer> toolchainDiscoverers

    @Override
    void apply(Project project) {
        def rootInstallTask = project.tasks.register("installToolchain", ToolchainInstallTask) { ToolchainInstallTask task ->
            task.group = "GradleRIO"
            task.description = "Install the C++ FRC Toolchain for this system"
        }

        project.extensions.create("wpiToolchain", WPIToolchainExtension)

        // Cancel all other tasks when installing the toolchain, if the toolchain is ready
        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            def installTask = graph.allTasks.find { Task t -> t instanceof ToolchainInstallTask } as ToolchainInstallTask
            if (installTask != null && installTask.needsInstall()) {
                project.tasks.each { Task t ->
                    if (!(t instanceof ToolchainInstallTask))
                        t.setEnabled(false)
                }

                def cancelled = graph.allTasks.findAll { Task t -> !(t instanceof ToolchainInstallTask) }
                if (cancelled.size() > 0) {
                    println("Installing Toolchain requires being the only task running. Cancelling the following task(s):")
                    cancelled.each { Task t ->
                        println("- " + t.name)
                    }
                }
            }
        }

        // Mostly for diagnostics
        def toolchainExplainTask = project.tasks.register("explainToolchains") { Task task ->
            task.group = "GradleRIO"
            task.description = "Explain FRC Toolchain Installs"

            task.doLast {
                println explainToolchains()
            }
        }
    }

    void createToolchainDiscoverers(Project project) {
        toolchainDiscoverers = [] as ArrayList
        WPIExtension wpiExtension = project.extensions.getByType(WPIExtension)

        // FRC Home
        def frcHome = wpiExtension.frcHome
        toolchainDiscoverers << new ToolchainDiscoverer("FRC Home", project, new File(frcHome))

        // GradleRIO ~/.gradle/gradlerio
        toolchainDiscoverers << new ToolchainDiscoverer(discovererGradleRIO, project, new File(GradleRIOPlugin.globalDirectory, "toolchains"))

        // System Path
        def os = new ByteArrayOutputStream()
        project.exec { ExecSpec spec ->
            def tool = ToolchainDiscoverer.composeTool("g++")
            if (OperatingSystem.current().isWindows())
                spec.commandLine("where.exe", tool)
            else
                spec.commandLine("which", tool)

            spec.standardOutput = os
            spec.ignoreExitValue = true
        }
        def paths = os.toString().trim().split("\n").collect { String path -> path.trim() }.findAll { String path -> !path.empty } as List<String>
        paths.eachWithIndex { String path, int index ->
            toolchainDiscoverers << new ToolchainDiscoverer("System Path " + index, project, new File(path).parentFile.parentFile)
        }

        // Populate Version Information
        toolchainDiscoverers.each { ToolchainDiscoverer t ->
            t.configureVersionChecking(wpiExtension.toolchainVersionLow, wpiExtension.toolchainVersionHigh)
        }
    }

    public static AbstractToolchainInstaller getActiveInstaller() {
        def toolchains = toolchainInstallers.findAll { t ->
            return t.installable()
        }
        if (toolchains.empty) return null;
        return toolchains.first()
    }

    public ToolchainDiscoverer maybeDiscoverRoborioToolchain() {
        return toolchainDiscoverers.find { ToolchainDiscoverer t -> t.valid() }
    }

    public ToolchainDiscoverer discoverRoborioToolchain() {
        def d = maybeDiscoverRoborioToolchain()
        if (d == null) {
            Logger.getLogger(this.class).info(explainToolchains())
            throw new ToolchainNotFoundException(notoolchainMessage)
        }
        return d
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

    String explainToolchains() {
        TreeVisitor<String> formatter = new TreeFormatter()
        toolchainDiscoverers.each { ToolchainDiscoverer tcd ->
            formatter.node(tcd.name)
            formatter.startChildren()
            tcd.explain(formatter)
            formatter.endChildren()
        }
        return formatter.toString()
    }

    static class WPIToolchainRules extends RuleSource {
        @Defaults
        void addToolchains(NativeToolChainRegistryInternal toolChainRegistry, ServiceRegistry serviceRegistry, ExtensionContainer extContainer) {
            final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
            final ExecActionFactory execActionFactory = serviceRegistry.get(ExecActionFactory.class);
            final CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory = serviceRegistry.get(CompilerOutputFileNamingSchemeFactory.class);
            final Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            final BuildOperationExecutor buildOperationExecutor = serviceRegistry.get(BuildOperationExecutor.class);
            final CompilerMetaDataProviderFactory metaDataProviderFactory = serviceRegistry.get(CompilerMetaDataProviderFactory.class);
            final WorkerLeaseService workerLeaseService = serviceRegistry.get(WorkerLeaseService.class);
            final SystemLibraryDiscovery standardLibraryDiscovery = serviceRegistry.get(SystemLibraryDiscovery.class);

            final Project project = extContainer.getByType(GradleRIOPlugin.ProjectWrapper).project

            project.plugins.getPlugin(WPIToolchainPlugin).createToolchainDiscoverers(project)

            toolChainRegistry.registerFactory(WPIRoboRioGcc.class, new NamedDomainObjectFactory<WPIRoboRioGcc>() {
                public WPIRoboRioGcc create(String name) {
                    return instantiator.newInstance(WPIRoboRioGcc.class, project, instantiator, name, buildOperationExecutor, OperatingSystem.current(), fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory, metaDataProviderFactory, workerLeaseService, standardLibraryDiscovery);
                }
            });
            toolChainRegistry.registerDefaultToolChain('roborioGcc', WPIRoboRioGcc)
        }

        @BinaryTasks
        void createNativeStripTasks(final ModelMap<Task> tasks, final ExtensionContainer extContainer, final NativeBinarySpec binary) {
            final Project project = extContainer.getByType(GradleRIOPlugin.ProjectWrapper).project
            if(extContainer.getByType(WPIToolchainExtension).skipStrip) {
                return
            }
            WPIRoboRioGcc gcc = null
            if (binary.toolChain instanceof WPIRoboRioGcc) {
                gcc = (WPIRoboRioGcc)binary.toolChain
            } else {
                return
            }
            Task rawLinkTask = null
            if (binary instanceof SharedLibraryBinarySpec) {
                rawLinkTask = ((SharedLibraryBinarySpec)binary).tasks.link
            } else if (binary instanceof NativeExecutableBinarySpec) {
                rawLinkTask = ((NativeExecutableBinarySpec)binary).tasks.link
            }
            if (!(rawLinkTask instanceof AbstractLinkTask)) {
                return
            }
            AbstractLinkTask linkTask = (AbstractLinkTask)rawLinkTask

            linkTask.doLast {
                def mainFile = linkTask.linkedFile.get().asFile

                if (mainFile.exists()) {
                    def mainFileStr = mainFile.toString()
                    def debugFile = mainFileStr + '.debug'

                    WPIToolchainPlugin plugin = project.plugins.getPlugin(WPIToolchainPlugin)
                    def tc = plugin.discoverRoborioToolchain()
                    def binDir = tc.binDir().get().toString()

                    def objcopyOptional = tc.tool('objcopy')
                    def stripOptional = tc.tool('strip')
                    if (!objcopyOptional.isPresent() || !stripOptional.isPresent()) {
                        def logger = ETLoggerFactory.INSTANCE.create("NativeBinaryStrip")
                        logger.logError('Failed to strip binaries because of unknown tool objcopy and strip')
                        return
                    }

                    def objcopy = tc.tool('objcopy').get().toString()
                    def strip = tc.tool('strip').get().toString()

                    project.exec { ExecSpec ex ->
                        ex.commandLine objcopy, '--only-keep-debug', mainFileStr, debugFile
                    }
                    project.exec { ExecSpec ex ->
                        ex.commandLine strip, '-g', mainFileStr
                    }
                    project.exec { ExecSpec ex ->
                        ex.commandLine objcopy, "--add-gnu-debuglink=$debugFile", mainFileStr
                    }
                }
            }
        }

        @Mutate
        void addPlatform(PlatformContainer platforms) {
            def roborio = platforms.maybeCreate('roborio', NativePlatform)
            roborio.architecture('arm')
            roborio.operatingSystem('linux')

            def desktop = platforms.maybeCreate('desktop', NativePlatform)
            System.getProperty("os.arch") == 'amd64' ? desktop.architecture('x86_64') : desktop.architecture('x86')

            def anyArm = platforms.maybeCreate('anyArm', NativePlatform)
            anyArm.architecture('arm')
        }

        @Mutate
        void addBinaryFlags(BinaryContainer binaries) {
            binaries.withType(NativeBinarySpec) { NativeBinarySpec bin ->
                if (!(bin.toolChain in VisualCpp)) {
                    bin.cppCompiler.args << "-std=c++1y" << '-g'
                } else {
                    bin.cppCompiler.args << '/Zi' << '/EHsc' << '/DNOMINMAX'
                    bin.linker.args << '/DEBUG:FULL'
                }
                null
            }
        }
    }
}
