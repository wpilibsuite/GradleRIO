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
    private WPIExtension wpiExtension

    @Override
    void install(Project project) {
        wpiExtension = project.extensions.getByType(WPIExtension)
        List<String> desiredVersion = wpiExtension.toolchainVersion.split("-") as List<String>
        URL src = WPIToolchainPlugin.toolchainDownloadURL(wpiExtension.toolchainTag, "FRC-${desiredVersion.first()}-Windows-Toolchain-${desiredVersion.last()}.zip")
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
        File installDir = WPIToolchainPlugin.toolchainInstallDirectory(wpiExtension.frcYear)
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.copy { CopySpec c ->
            c.from(new File(extrDir, "frc${desiredVersion.first()}/roborio"))
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
        return WPIToolchainPlugin.toolchainInstallDirectory(wpiExtension.frcYear)
    }
}
