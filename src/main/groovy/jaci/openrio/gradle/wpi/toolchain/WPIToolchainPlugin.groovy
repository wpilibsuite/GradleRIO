package jaci.openrio.gradle.wpi.toolchain

import de.undercouch.gradle.tasks.download.DownloadTaskPlugin
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.toolchain.install.AbstractToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.LinuxToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.MacOSToolchainInstaller
import jaci.openrio.gradle.wpi.toolchain.install.NoToolchainInstallersException
import jaci.openrio.gradle.wpi.toolchain.install.WindowsToolchainInstaller
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class WPIToolchainPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        List<AbstractToolchainInstaller> toolchainInstallers = []
        project.extensions.add("toolchainInstallers", toolchainInstallers)

        toolchainInstallers.add(new WindowsToolchainInstaller())
        toolchainInstallers.add(new MacOSToolchainInstaller())
        toolchainInstallers.add(new LinuxToolchainInstaller())

        def rootInstallTask = project.task("installToolchain") { Task task ->
            task.group = "GradleRIO"
            task.description = "Install the C++ FRC Toolchain for this system"

            task.doLast {
                def toolchains = toolchainInstallers.findAll { t ->
                    return t.installable()
                }

                if (toolchains.empty) {
                    throw new NoToolchainInstallersException("Cannot install Toolchain! (No Installers for this Platform)")
                } else {
                    toolchains.first().installToolchain(task.project)
                }
            }
        }
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
