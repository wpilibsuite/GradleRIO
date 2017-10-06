package jaci.openrio.gradle.wpi.toolchain.install

import jaci.openrio.gradle.wpi.WPIExtension
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class WindowsToolchainInstallerTask extends AbstractToolchainInstallerTask {
    @Override
    void install(Project project) {
        List<String> desiredVersion = project.extensions.getByType(WPIExtension).toolchainVersion.split("-")
        URL src = WPIToolchainPlugin.toolchainDownloadURL("FRC-${desiredVersion.first()}-Windows-Toolchain-${desiredVersion.last()}.zip")
        File dst = new File(WPIToolchainPlugin.toolchainDownloadDirectory(), "win-${desiredVersion.join("-")}.zip")
        dst.parentFile.mkdirs()

        if (!dst.exists()) {
            println "Downloading..."
            src.withInputStream { i -> dst.withOutputStream { o -> o << i } }
        } else {
            println "Already downloaded!"
        }

        println "Extracting..."
        File extrDir = new File(WPIToolchainPlugin.toolchainInstallDirectory(), "win")
        if (extrDir.exists()) extrDir.deleteDir()
        extrDir.mkdirs()

        project.copy { c ->
            c.from(project.zipTree(dst))
            c.into(extrDir)
        }

        println "Done!"
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isWindows()
    }

    @Override
    File toolchainRoot() {
        return new File(WPIToolchainPlugin.toolchainInstallDirectory(), "win/frc")
    }
}
