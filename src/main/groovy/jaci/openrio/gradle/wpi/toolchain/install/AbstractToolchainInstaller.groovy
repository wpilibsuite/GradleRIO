package jaci.openrio.gradle.wpi.toolchain.install

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

abstract class AbstractToolchainInstaller {
    abstract void install(Project project)
    abstract boolean targets(OperatingSystem os)
    abstract String installerPlatform()

    boolean installable() {
        return targets(OperatingSystem.current())
    }

    void installToolchain(Project project) {
        println("Installing FRC Toolchain for platform ${installerPlatform()}...")
        install(project)
    }
}
