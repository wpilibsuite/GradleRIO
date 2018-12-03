package edu.wpi.first.toolchain;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;

public class InstallToolchainTask extends DefaultTask {

    private ToolchainDescriptor desc;

    public boolean requiresInstall() {
        return desc.discover() == null || getProject().hasProperty("toolchain-install-force");
    }

    public void setDescriptor(ToolchainDescriptor desc) {
        this.desc = desc;
    }

    public ToolchainDescriptor getDescriptor() {
        return desc;
    }

    public String getDescriptorName() {
        return desc.getName();
    }

    @TaskAction
    public void install() {
        AbstractToolchainInstaller installer = desc.getInstaller();

        if (installer == null) {
            throw new GradleException("No Toolchain Installers exist for " + desc.getName() + " on platform " + OperatingSystem.current().getName());
        } else {
            if (requiresInstall()) {
                installer.install(getProject());
            } else {
                System.out.println("Valid Toolchain found! " + desc.discover().getName());
                System.out.println("Force re-install with -Ptoolchain-install-force");
            }
        }
    }

}
