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

    List<String> print(String platform, boolean debug) {
        return ["edu.wpi.first.halsim:halsim-print:${wpi.wpilibVersion}:${platform}${debug ? 'debug' : ''}@zip".toString()]
    }

    List<String> nt_ds(String platform, boolean debug) {
        return ["edu.wpi.first.halsim.ds:halsim-ds-nt:${wpi.wpilibVersion}:${platform}${debug ? 'debug' : ''}@zip".toString()]
    }

    List<String> lowfi(String platform, boolean debug) {
        return ["edu.wpi.first.halsim:halsim-lowfi:${wpi.wpilibVersion}:${platform}${debug ? 'debug' : ''}@zip".toString()]
    }

    List<String> driverstation(String platform, boolean debug) {
        return ["edu.wpi.first.halsim:halsim_ds_socket:${wpi.wpilibVersion}:${platform}${debug ? 'debug' : ''}@zip".toString()]
    }

    List<String> gui(String platform, boolean debug) {
        return ["edu.wpi.first.halsim:halsim_gui:${wpi.wpilibVersion}:${platform}${debug ? 'debug' : ''}@zip".toString()]
    }
}
