package edu.wpi.first.gradlerio.wpi.simulation

import groovy.transform.CompileStatic
import javax.inject.Inject
import org.gradle.api.Named

@CompileStatic
class HalSimExtension implements Named {
    String name
    String baseArtifact

    @Inject
    HalSimExtension(String name) {
        this.name = name
    }
}
