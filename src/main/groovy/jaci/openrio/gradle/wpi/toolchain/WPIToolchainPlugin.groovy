package jaci.openrio.gradle.wpi.toolchain

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.toolchain.discover.AbstractToolchainDiscoverer
import jaci.openrio.gradle.wpi.toolchain.discover.FRCHomeToolchainDiscoverer
import jaci.openrio.gradle.wpi.toolchain.discover.GradleToolchainDiscoverer
import jaci.openrio.gradle.wpi.toolchain.discover.SystemPathToolchainDiscoverer
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
import org.gradle.process.internal.ExecActionFactory
import org.gradle.util.TreeVisitor

@CompileStatic
class WPIToolchainPlugin implements Plugin<Project> {

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

    List<AbstractToolchainDiscoverer> toolchainDiscoverers

    @Override
    void apply(Project project) {
        toolchainDiscoverers = [
            new FRCHomeToolchainDiscoverer(project),
            new GradleToolchainDiscoverer(project),
            new SystemPathToolchainDiscoverer(project)
        ]

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

    public static AbstractToolchainInstaller getActiveInstaller() {
        def toolchains = toolchainInstallers.findAll { t ->
            return t.installable()
        }
        if (toolchains.empty) return null;
        return toolchains.first()
    }

    public AbstractToolchainDiscoverer discoverRoborioToolchain() {
        def d = toolchainDiscoverers.find { AbstractToolchainDiscoverer t -> t.exists() }
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
        toolchainDiscoverers.each { AbstractToolchainDiscoverer tcd ->
            formatter.node(tcd.class.simpleName)
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

            toolChainRegistry.registerFactory(WPIRoboRioGcc.class, new NamedDomainObjectFactory<WPIRoboRioGcc>() {
                public WPIRoboRioGcc create(String name) {
                    return instantiator.newInstance(WPIRoboRioGcc.class, project, instantiator, name, buildOperationExecutor, OperatingSystem.current(), fileResolver, execActionFactory, compilerOutputFileNamingSchemeFactory, metaDataProviderFactory, workerLeaseService, standardLibraryDiscovery);
                }
            });
            toolChainRegistry.registerDefaultToolChain('roborioGcc', WPIRoboRioGcc)
        }
    }
}
