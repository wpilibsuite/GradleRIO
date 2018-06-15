package jaci.openrio.gradle.wpi.toolchain.discover

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@InheritConstructors
@CompileStatic
class FRCHomeToolchainDiscoverer extends AbstractToolchainDiscoverer {

    final String homeEnv = "FRC_2018ALPHA_HOME"

    @Override
    Optional<File> rootDir() {
        def envvar = System.getenv(homeEnv)
        if (envvar == null || envvar.empty)
            return Optional.empty()

        return optFile(new File(envvar, "gcc"))
    }

}
