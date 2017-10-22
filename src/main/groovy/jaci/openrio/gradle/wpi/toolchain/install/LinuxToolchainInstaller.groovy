package jaci.openrio.gradle.wpi.toolchain.install

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class LinuxToolchainInstaller extends AbstractToolchainInstaller {
    @Override
    void install(Project project) {
        def filecontents = [
            '#!/bin/bash',
            'apt-add-repository ppa:wpilib/toolchain',
            'apt update',
            'apt install frc-toolchain'
        ]
        def file = project.rootProject.file('build/LINUX_TOOLCHAIN_INSTALL.sh')
        file.mkdirs()
        file.text = filecontents.join('\n')
        println "Run `sudo ./LINUX_TOOLCHAIN_INSTALL.sh` in `build` in order to install toolchain"
    }

    @Override
    boolean targets(OperatingSystem os) {
        return os.isLinux()
    }

    @Override
    String installerPlatform() {
        return "Linux"
    }

    @Override
    File sysrootLocation() {
        return new File("/")             // TODO: Determine Sysroot Location
    }
}
