package edu.wpi.first.gradlerio.wpi.toolchain.install

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

@CompileStatic
abstract class AbstractToolchainInstaller {
    abstract void install(Project project)
    abstract boolean targets(OperatingSystem os)
    abstract String installerPlatform()
    abstract File sysrootLocation()

    boolean installable() {
        return targets(OperatingSystem.current())
    }

    void installToolchain(Project project) {
        println("Installing FRC Toolchain for platform ${installerPlatform()}...")
        install(project)
    }
}
