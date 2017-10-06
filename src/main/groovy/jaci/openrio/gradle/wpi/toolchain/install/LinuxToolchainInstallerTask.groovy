package jaci.openrio.gradle.wpi.toolchain.install

import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class LinuxToolchainInstallerTask extends AbstractToolchainInstallerTask {
    @Override
    void install(Project project) {
        try {
            project.exec { e ->
                e.commandLine 'apt-add-repository'
                e.args 'ppa:wpilib/toolchain'
            }
            project.exec { e ->
                e.commandLine 'apt'
                e.args 'update'
            }
            project.exec { e ->
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
    File toolchainRoot() {
        return null     // Assume toolchain is on PATH
    }
}
