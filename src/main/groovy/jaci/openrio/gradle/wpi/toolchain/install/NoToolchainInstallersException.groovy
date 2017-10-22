package jaci.openrio.gradle.wpi.toolchain.install

import groovy.transform.CompileStatic

@CompileStatic
class NoToolchainInstallersException extends RuntimeException {
    NoToolchainInstallersException(String msg) {
        super(msg)
    }
}
