package edu.wpi.first.toolchain;

import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.util.TreeVisitor;

public class ToolchainExtension extends DefaultNamedDomainObjectSet<ToolchainDescriptor> {

    public ToolchainExtension() {
        super(ToolchainDescriptor.class, DirectInstantiator.INSTANCE);
    }

    public void explain(TreeVisitor<String> visitor) {
        for (ToolchainDescriptor desc : this) {
            visitor.node(desc.getName());
            visitor.startChildren();
            visitor.node("Selected: " + desc.discover().getName());
            desc.explain(visitor);
            visitor.endChildren();
        }
    }
}
