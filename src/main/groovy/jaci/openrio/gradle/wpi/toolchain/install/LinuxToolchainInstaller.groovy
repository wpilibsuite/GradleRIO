package jaci.openrio.gradle.wpi.toolchain.install

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

@CompileStatic
class LinuxToolchainInstaller extends AbstractToolchainInstaller {
    @Override
    void install(Project project) {
        try {
            project.exec { ExecSpec e ->
                e.commandLine 'apt-add-repository'
                e.args 'ppa:wpilib/toolchain'
            }
            project.exec { ExecSpec e ->
                e.commandLine 'apt'
                e.args 'update'
            }
            project.exec { ExecSpec e ->
                e.commandLine 'apt'
                e.args 'install', 'frc-toolchain'
            }
        } catch (all) {
            throw new NoToolchainInstallersException("Linux installer only works on debian (apt) platforms! If on debian, run with sudo? See http://first.wpi.edu/FRC/roborio/toolchains/FRCLinuxToolchain.txt")
        }
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isLinux()
    }

    @Override
    String installerPlatform() {
        return "Linux"
    }
}
