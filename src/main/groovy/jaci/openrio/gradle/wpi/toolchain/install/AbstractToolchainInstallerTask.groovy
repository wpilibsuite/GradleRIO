package jaci.openrio.gradle.wpi.toolchain.install

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

abstract class AbstractToolchainInstallerTask extends DefaultTask {
    abstract void install(Project project)
    abstract boolean targets(OperatingSystem os)
    abstract File toolchainRoot()

    boolean installable() {
        return targets(OperatingSystem.current())
    }

    @TaskAction
    void installToolchain() {
        if (!installable()) {
            throw new StopExecutionException()
        }
        println("Installing FRC Toolchain for platform ${OperatingSystem.current().name}...")
        install(project)
    }
}
