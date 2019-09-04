package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project

import javax.inject.Inject

@CompileStatic
public class WPISimDepsExtension {

    final WPIExtension wpi

    @Inject
    WPISimDepsExtension(WPIExtension wpi) {
        this.wpi = wpi
    }

    List<String> print() {
        return ["edu.wpi.first.halsim:halsim-print:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
    }

    List<String> nt_ds() {
        return ["edu.wpi.first.halsim.ds:halsim-ds-nt:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
    }

    List<String> lowfi() {
        return ["edu.wpi.first.halsim:halsim-lowfi:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
    }
}
