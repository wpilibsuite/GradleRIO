package edu.wpi.first.gradlerio.ide

import groovy.transform.CompileStatic
import org.gradle.nativeplatform.NativeExecutableBinarySpec

@CompileStatic
class EditorConfigurationExtension {
    List<NativeExecutableBinarySpec> _binaries = []
}
