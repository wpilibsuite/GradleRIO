package edu.wpi.first.gradlerio.ide

import groovy.transform.CompileStatic
import org.gradle.nativeplatform.NativeBinarySpec

@CompileStatic
class ClionExtension {
    List<ClionBinarySpec> _binaries = []

    static class ClionBinarySpec {
        NativeBinarySpec binary
        File file

        ClionBinarySpec(NativeBinarySpec bin, File file) {
            this.binary = bin
            this.file = file
        }
    }
}
