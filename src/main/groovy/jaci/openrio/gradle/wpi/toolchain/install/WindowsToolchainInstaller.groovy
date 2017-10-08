package jaci.openrio.gradle.wpi.toolchain.install

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadAction
import jaci.openrio.gradle.wpi.WPIExtension
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class WindowsToolchainInstaller extends AbstractToolchainInstaller {
    @Override
    void install(Project project) {
        List<String> desiredVersion = project.extensions.getByType(WPIExtension).toolchainVersion.split("-")
        URL src = WPIToolchainPlugin.toolchainDownloadURL("FRC-${desiredVersion.first()}-Windows-Toolchain-${desiredVersion.last()}.zip")
        File dst = new File(WPIToolchainPlugin.toolchainDownloadDirectory(), "win-${desiredVersion.join("-")}.zip")
        dst.parentFile.mkdirs()

        println "Downloading..."
        def da = new DownloadAction(project)
        da.with { d ->
            d.src src
            d.dest dst
            d.overwrite false
            d.onlyIfModified true
        }
        da.execute()
        if (da.upToDate) {
            println "Already Downloaded!"
        }

        println "Extracting..."
        File extrDir = new File(WPIToolchainPlugin.toolchainExtractDirectory(), "win")
        if (extrDir.exists()) extrDir.deleteDir()
        extrDir.mkdirs()

        project.copy { c ->
            c.from(project.zipTree(dst))
            c.into(extrDir)
        }

        println "Copying..."
        File installDir = WPIToolchainPlugin.toolchainInstallDirectory()
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.copy { c ->
            c.from(new File(extrDir, "frc"))
            c.into(installDir)
        }

        println "Done!"
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isWindows()
    }

    @Override
    String installerPlatform() {
        return "Windows"
    }
}
