package edu.wpi.first.toolchain;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.util.TreeVisitor;

public interface ToolchainDescriptorBase extends Named {

    public void setToolchainPlatforms(String... platforms);

    public NamedDomainObjectSet<ToolchainDiscoverer> getDiscoverers();

    public DomainObjectSet<AbstractToolchainInstaller> getInstallers();

    public void explain(TreeVisitor<String> visitor);

    public ToolchainDiscoverer discover();

    public AbstractToolchainInstaller getInstaller();

    public String getToolchainName();

    public String getInstallTaskName();

    public boolean isOptional();

    public void setOptional(boolean optional);

    public String[] getToolchainPlatforms();

    public ToolchainRegistrarBase getRegistrar();
}
