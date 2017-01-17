package jaci.openrio.gradle

import org.gradle.api.*;
import groovy.util.*;

import jaci.openrio.gradle.frc.DeployPlugin;
import jaci.openrio.gradle.frc.WPIPlugin;

class GradleRIO implements Plugin<Project> {
    void apply(Project project) {
        project.configurations.maybeCreate("nativeLib")
        project.configurations.maybeCreate("nativeZip")
        
        project.with {
            apply plugin: DeployPlugin
            apply plugin: WPIPlugin
        }
    }

    // ~/.gradle
    static File getGlobalDirectory() {
        return new File("${System.getProperty('user.home')}/.gradle", "gradlerioc")
    }
}