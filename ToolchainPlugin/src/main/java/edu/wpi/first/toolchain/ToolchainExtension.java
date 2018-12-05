package edu.wpi.first.toolchain;

import edu.wpi.first.toolchain.raspbian.RaspbianToolchainPlugin;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.util.TreeVisitor;

public class ToolchainExtension extends DefaultNamedDomainObjectSet<ToolchainDescriptor> {

    private Project project;

    public boolean registerPlatforms = true;

    public ToolchainExtension(Project project) {
        super(ToolchainDescriptor.class, DirectInstantiator.INSTANCE);
        this.project = project;
    }

    public void withRoboRIO() {
        project.getPluginManager().apply(RoboRioToolchainPlugin.class);
    }

    public void withRaspbian() {
        project.getPluginManager().apply(RaspbianToolchainPlugin.class);
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
