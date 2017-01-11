package jaci.openrio.gradle.frc

import org.gradle.api.*;
import groovy.util.*;

class DeployPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("frc", FRCExtension)
    }
}