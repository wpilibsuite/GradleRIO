package edu.wpi.first.gradlerio.wpi.toolchain

import edu.wpi.first.gradlerio.wpi.toolchain.install.AbstractToolchainInstaller
import edu.wpi.first.gradlerio.wpi.toolchain.install.LinuxToolchainInstaller
import edu.wpi.first.gradlerio.wpi.toolchain.install.MacOSToolchainInstaller
import edu.wpi.first.gradlerio.wpi.toolchain.install.NoToolchainInstallersException
import edu.wpi.first.gradlerio.wpi.toolchain.install.WindowsToolchainInstaller
import groovy.transform.CompileStatic
import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.wpi.WPIExtension
import jaci.openrio.gradle.wpi.toolchain.install.*
import org.apache.log4j.Logger
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.text.TreeFormatter
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecActionFactory
import org.gradle.util.TreeVisitor

@CompileStatic
class WPIToolchainPlugin implements Plugin<Project> {

    final String homeEnv = "FRC_2018ALPHA_HOME"

    @CompileStatic
    class ToolchainNotFoundException extends RuntimeException {
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

        // Mostly for diagnostics
        def toolchainExplainTask = project.task("explainToolchains") { Task task ->
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
        def envvar = System.getenv(homeEnv)
        toolchainDiscoverers << new ToolchainDiscoverer("FRC Home", project, envvar == null ? null : new File(envvar))

        // GradleRIO ~/.gradle/gradlerio
        toolchainDiscoverers << new ToolchainDiscoverer("GradleRIO Home", project, new File(GradleRIOPlugin.globalDirectory, "toolchains"))

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

    public ToolchainDiscoverer discoverRoborioToolchain() {
        def d = toolchainDiscoverers.find { ToolchainDiscoverer t -> t.valid() }
        if (d == null) {
            Logger.getLogger(this.class).info(explainToolchains())
            throw new ToolchainNotFoundException("No valid toolchain(s) found! Information dumped to info log (run with --info)")
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
        @Mutate
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
    }
}
