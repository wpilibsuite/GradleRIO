package jaci.openrio.gradle.frc

import jaci.gradle.deployers.Deployer
import jaci.openrio.gradle.frc.ext.FRCNative
import org.gradle.api.Plugin
import org.gradle.api.Project

class FRCNativePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) { }

    void configureDeployer(Project project, Deployer deployer, FRCNative frcNative) {

    }
}
