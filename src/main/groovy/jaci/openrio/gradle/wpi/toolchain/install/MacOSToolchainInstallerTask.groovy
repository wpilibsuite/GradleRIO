package jaci.openrio.gradle.wpi.toolchain.install

import jaci.openrio.gradle.wpi.WPIExtension
import jaci.openrio.gradle.wpi.toolchain.WPIToolchainPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.internal.impldep.org.apache.maven.toolchain.Toolchain
import org.gradle.internal.os.OperatingSystem

class MacOSToolchainInstallerTask extends AbstractToolchainInstallerTask {

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

        File installDir = new File(WPIToolchainPlugin.toolchainInstallDirectory(), "macOS")
        if (installDir.exists()) installDir.deleteDir()
        installDir.mkdirs()

        project.exec { e ->
            e.commandLine('pax')
            e.workingDir(installDir)
            e.args('-r', '-f', archiveFile.absolutePath)
        }

        println "Done!"
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isMacOsX()
    }

    @Override
    File toolchainRoot() {
        return new File(WPIToolchainPlugin.toolchainInstallDirectory(), "macOS/usr/local")
    }
}
