package jaci.openrio.gradle.wpi.toolchain.install

import jaci.openrio.gradle.wpi.WPIExtension
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class MacOSToolchainInstaller extends AbstractToolchainInstaller {

    @Override
    void install(Project project) {
        List<String> desiredVersion = project.extensions.getByType(WPIExtension).toolchainVersion.split("-")
        URL src = WPIToolchainPlugin.toolchainDownloadURL("FRC-${desiredVersion.first()}-OSX-Toolchain-${desiredVersion.last()}.pkg.tar.gz")
        File dst = new File(WPIToolchainPlugin.toolchainDownloadDirectory(), "macOS-${desiredVersion.join("-")}.pkg.tar.gz")
        dst.parentFile.mkdirs()

        if (!dst.exists()) {
            println "Downloading..."
            src.withInputStream { i -> dst.withOutputStream { o -> o << i } }
        } else {
            println "Already downloaded!"
        }

        println "Extracting..."
        File extrDir = new File(WPIToolchainPlugin.toolchainExtractDirectory(), "macOS")
        if (extrDir.exists()) extrDir.deleteDir()
        extrDir.mkdirs()

        project.copy { c ->
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

        project.exec { e ->
            e.commandLine('pax')
            e.workingDir(paxDir)
            e.args('-r', '-f', archiveFile.absolutePath)
        }

        println "Copying..."
        File installDir = WPIToolchainPlugin.toolchainInstallDirectory()
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.copy { c ->
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
}
