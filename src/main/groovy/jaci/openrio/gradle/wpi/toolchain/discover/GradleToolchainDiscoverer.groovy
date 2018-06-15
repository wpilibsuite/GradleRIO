package jaci.openrio.gradle.wpi.toolchain.discover

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.openrio.gradle.GradleRIOPlugin

@InheritConstructors
@CompileStatic
class GradleToolchainDiscoverer extends AbstractToolchainDiscoverer {

    @Override
    Optional<File> rootDir() {
        return optFile(new File(GradleRIOPlugin.globalDirectory, "toolchains"))
    }

}
