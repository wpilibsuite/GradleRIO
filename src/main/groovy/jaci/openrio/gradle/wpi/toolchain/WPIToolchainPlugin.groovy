package jaci.openrio.gradle.wpi.toolchain

import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.toolchain.install.AbstractToolchainInstallerTask
import jaci.openrio.gradle.wpi.toolchain.install.LinuxToolchainInstallerTask
import jaci.openrio.gradle.wpi.toolchain.install.MacOSToolchainInstallerTask
import jaci.openrio.gradle.wpi.toolchain.install.NoToolchainInstallersException
import jaci.openrio.gradle.wpi.toolchain.install.WindowsToolchainInstallerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.impldep.org.apache.maven.toolchain.Toolchain
import org.gradle.internal.os.OperatingSystem

class WPIToolchainPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def rootInstallTask = project.task("installToolchain") { Task task ->
            task.group = "GradleRIO"
            task.description = "Install the C++ FRC Toolchain for this system"

            task.doLast {
                def emptyToolchains = task.getDependsOn().findAll { t ->
                    return (t instanceof AbstractToolchainInstallerTask && t.installable())
                }.empty

                if (emptyToolchains) {
                    throw new NoToolchainInstallersException("Cannot install Toolchain! (No Installers for this Platform)")
                }
            }
        }

        project.tasks.withType(AbstractToolchainInstallerTask).whenTaskAdded { task ->
            rootInstallTask.dependsOn(task)
        }

        project.tasks.create("installToolchainWindows", WindowsToolchainInstallerTask)
        project.tasks.create("installToolchainMacOS", MacOSToolchainInstallerTask)
        project.tasks.create("installToolchainLinux", LinuxToolchainInstallerTask)
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
}
