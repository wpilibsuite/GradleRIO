package edu.wpi.first.toolchain;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.util.TreeVisitor;

public class ToolchainDescriptor implements Named {

    private String name;
    private String toolchainName;
    private String[] platforms;
    private boolean optional;
    private NamedDomainObjectSet<ToolchainDiscoverer> discoverers;
    private DomainObjectSet<AbstractToolchainInstaller> installers;

    private ToolchainRegistrar registrar;

    public ToolchainDescriptor(String name, String toolchainName, ToolchainRegistrar registrar) {
        this.name = name;
        this.platforms = null;
        this.optional = false;
        this.registrar = registrar;
        this.toolchainName = toolchainName;
        this.discoverers = new DefaultNamedDomainObjectSet<ToolchainDiscoverer>(ToolchainDiscoverer.class, DirectInstantiator.INSTANCE);
        this.installers = new DefaultDomainObjectSet<AbstractToolchainInstaller>(AbstractToolchainInstaller.class);
    }

    public void setToolchainPlatforms(String... platforms) {
        this.platforms = platforms;
    }

    public NamedDomainObjectSet<ToolchainDiscoverer> getDiscoverers() {
        return discoverers;
    }

    public DomainObjectSet<AbstractToolchainInstaller> getInstallers() {
        return installers;
    }

    public ToolchainDiscoverer discover() {
        return discoverers.stream().filter(ToolchainDiscoverer::valid).findFirst().orElse(null);
    }

    public AbstractToolchainInstaller getInstaller() {
        return installers.stream().filter(AbstractToolchainInstaller::installable).findFirst().orElse(null);
    }

    public void explain(TreeVisitor<String> visitor) {
        for (ToolchainDiscoverer discoverer : discoverers) {
            visitor.node(discoverer.getName());
            visitor.startChildren();
            discoverer.explain(visitor);
            visitor.endChildren();
        }
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String[] getToolchainPlatforms() {
        return platforms;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getToolchainName() {
        return toolchainName;
    }

    public String installTaskName() {
        return "install" + capitalize(getName()) + "Toolchain";
    }

    public ToolchainRegistrar getRegistrar() {
        return registrar;
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
