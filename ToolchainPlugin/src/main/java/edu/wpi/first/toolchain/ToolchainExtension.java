package edu.wpi.first.toolchain;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.util.TreeVisitor;

import edu.wpi.first.toolchain.configurable.ConfigurableGcc;
import edu.wpi.first.toolchain.configurable.CrossCompilerConfiguration;
import edu.wpi.first.toolchain.configurable.DefaultCrossCompilerConfiguration;
import edu.wpi.first.toolchain.raspbian.RaspbianToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;

public class ToolchainExtension {
    private final NamedDomainObjectContainer<CrossCompilerConfiguration> crossCompilers;
    private final NamedDomainObjectContainer<ToolchainDescriptor> toolchainDescriptors;

    private Project project;

    public boolean registerPlatforms = true;

    public ToolchainExtension(Project project) {
        this.project = project;

        crossCompilers = project.container(CrossCompilerConfiguration.class, name -> {
            return project.getObjects().newInstance(DefaultCrossCompilerConfiguration.class, name);
        });

        toolchainDescriptors = project.container(ToolchainDescriptor.class);

        crossCompilers.all(config -> {
            if (config.getToolchainDescriptor() == null) {
                ToolchainDescriptor descriptor = new ToolchainDescriptor(config.getName(),
                        config.getName() + "ConfiguredGcc",
                        new ToolchainRegistrar<ConfigurableGcc>(ConfigurableGcc.class, project));

                toolchainDescriptors.add(descriptor);

                project.afterEvaluate(proj -> {
                    descriptor.setToolchainPlatforms(config.getOperatingSystem() + config.getArchitecture());
                    descriptor.setOptional(config.getOptional());
                    descriptor.getDiscoverers().addAll(ToolchainDiscoverer.forSystemPath(project, name -> {
                        String exeSuffix = OperatingSystem.current().isWindows() ? ".exe" : "";
                        return config.getCompilerPrefix() + name + exeSuffix;
                    }));
                });
                config.setToolchainDescriptor(descriptor);
            } else {
                toolchainDescriptors.add(config.getToolchainDescriptor());
            }
        });

    }

    public void withRoboRIO() {
        project.getPluginManager().apply(RoboRioToolchainPlugin.class);
    }

    public void withRaspbian() {
        project.getPluginManager().apply(RaspbianToolchainPlugin.class);
    }

    public NamedDomainObjectContainer<ToolchainDescriptor> getToolchainDescriptors() {
        return toolchainDescriptors;
    }

    void toolchainDescriptors(final Action<? super NamedDomainObjectContainer<ToolchainDescriptor>> closure) {
        closure.execute(toolchainDescriptors);
    }

    public NamedDomainObjectContainer<CrossCompilerConfiguration> getCrossCompilers() {
        return crossCompilers;
    }

    void crossCompilers(final Action<? super NamedDomainObjectContainer<CrossCompilerConfiguration>> closure) {
        closure.execute(crossCompilers);
    }

    public void explain(TreeVisitor<String> visitor) {
        for (ToolchainDescriptor desc : toolchainDescriptors) {
            visitor.node(desc.getName());
            visitor.startChildren();
            visitor.node("Selected: " + desc.discover().getName());
            desc.explain(visitor);
            visitor.endChildren();
        }
    }
}
