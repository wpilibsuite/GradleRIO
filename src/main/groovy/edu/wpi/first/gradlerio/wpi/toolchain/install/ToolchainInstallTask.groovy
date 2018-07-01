package edu.wpi.first.gradlerio.wpi.toolchain.install

import edu.wpi.first.gradlerio.wpi.toolchain.ToolchainDiscoverer
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ToolchainInstallTask extends DefaultTask {

    boolean needsInstall() {
        def plugin = project.plugins.getPlugin(WPIToolchainPlugin)
        def discoverer = plugin.toolchainDiscoverers.find { ToolchainDiscoverer d -> d.name.equals(WPIToolchainPlugin.discovererGradleRIO) }
        def override = project.hasProperty("toolchain-install-force")

        return !discoverer.valid() || override
    }

    @TaskAction
    void install() {
        AbstractToolchainInstaller installer = WPIToolchainPlugin.getActiveInstaller()

        if (installer == null) {
            throw new NoToolchainInstallersException("Cannot install Toolchain! (No Installers for this Platform)")
        } else {
            if (needsInstall()) {
                installer.installToolchain(project)
            } else {
                println("Valid RoboRIO Toolchain for this platform is already installed!")
                println("Force re-install with -Ptoolchain-install-force")
            }
        }
    }

}
