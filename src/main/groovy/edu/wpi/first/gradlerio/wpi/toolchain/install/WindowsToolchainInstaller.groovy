package edu.wpi.first.gradlerio.wpi.toolchain.install

import de.undercouch.gradle.tasks.download.DownloadAction
import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class WindowsToolchainInstaller extends AbstractToolchainInstaller {
    @Override
    void install(Project project) {
        List<String> desiredVersion = project.extensions.getByType(WPIExtension).toolchainVersion.split("-") as List<String>
        URL src = WPIToolchainPlugin.toolchainDownloadURL("FRC-${desiredVersion.first()}-Windows-Toolchain-${desiredVersion.last()}.zip")
        File dst = new File(WPIToolchainPlugin.toolchainDownloadDirectory(), "win-${desiredVersion.join("-")}.zip")
        dst.parentFile.mkdirs()

        println "Downloading..."
        def da = new DownloadAction(project)
        da.with { DownloadAction d ->
            d.src src
            d.dest dst
            d.overwrite false
        }
        da.execute()
        if (da.upToDate) {
            println "Already Downloaded!"
        }

        println "Extracting..."
        File extrDir = new File(WPIToolchainPlugin.toolchainExtractDirectory(), "win")
        if (extrDir.exists()) extrDir.deleteDir()
        extrDir.mkdirs()

        project.copy { CopySpec c ->
            c.from(project.zipTree(dst))
            c.into(extrDir)
        }

        println "Copying..."
        File installDir = WPIToolchainPlugin.toolchainInstallDirectory()
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.copy { CopySpec c ->
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

    @Override
    File sysrootLocation() {
        return WPIToolchainPlugin.toolchainInstallDirectory()
    }
}
