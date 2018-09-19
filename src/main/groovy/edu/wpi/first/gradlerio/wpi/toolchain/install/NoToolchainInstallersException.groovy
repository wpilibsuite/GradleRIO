package edu.wpi.first.gradlerio.wpi.toolchain.install

import groovy.transform.CompileStatic

@CompileStatic
class NoToolchainInstallersException extends RuntimeException {
    NoToolchainInstallersException(String msg) {
        super(msg)
    }
}
