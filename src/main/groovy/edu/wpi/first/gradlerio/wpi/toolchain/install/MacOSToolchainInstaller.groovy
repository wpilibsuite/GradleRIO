package edu.wpi.first.gradlerio.wpi.toolchain.install

import de.undercouch.gradle.tasks.download.DownloadAction
import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.toolchain.WPIToolchainPlugin
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

@CompileStatic
class MacOSToolchainInstaller extends AbstractToolchainInstaller {

    @Override
    void install(Project project) {
        List<String> desiredVersion = project.extensions.getByType(WPIExtension).toolchainVersion.split("-") as List<String>
        URL src = WPIToolchainPlugin.toolchainDownloadURL("FRC-${desiredVersion.first()}-OSX-Toolchain-${desiredVersion.last()}.pkg.tar.gz")
        File dst = new File(WPIToolchainPlugin.toolchainDownloadDirectory(), "macOS-${desiredVersion.join("-")}.pkg.tar.gz")
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
        File extrDir = new File(WPIToolchainPlugin.toolchainExtractDirectory(), "macOS")
        if (extrDir.exists()) extrDir.deleteDir()
        extrDir.mkdirs()

        project.copy { CopySpec c ->
            c.from(project.tarTree(project.resources.gzip(dst)))
            c.into(extrDir)
        }

        File archiveFile = new File(extrDir, "Archive.pax")
        project.resources.gzip(new File(extrDir, "FRC ARM Toolchain.pkg/Contents/Archive.pax.gz")).read().with { i ->
            archiveFile.withOutputStream { o -> o << i }
        }

        File paxDir = new File(extrDir, "unpack")
        if (paxDir.exists()) paxDir.deleteDir()
        paxDir.mkdirs()

        project.exec { ExecSpec e ->
            e.commandLine('pax')
            e.workingDir(paxDir)
            e.args('-r', '-f', archiveFile.absolutePath)
        }

        println "Copying..."
        File installDir = WPIToolchainPlugin.toolchainInstallDirectory()
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.copy { CopySpec c ->
            c.from(new File(paxDir, "usr/local"))
            c.into(installDir)
        }

        println "Done!"
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isMacOsX()
    }

    @Override
    String installerPlatform() {
        return "MacOS"
    }

    @Override
    File sysrootLocation() {
        return new File(WPIToolchainPlugin.toolchainInstallDirectory(),'arm-frc-linux-gnueabi')
    }
}
