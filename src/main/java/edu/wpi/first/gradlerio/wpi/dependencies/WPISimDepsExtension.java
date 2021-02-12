package edu.wpi.first.gradlerio.wpi.dependencies;

import edu.wpi.first.gradlerio.wpi.WPIExtension;

import java.util.List;

import javax.inject.Inject;

public class WPISimDepsExtension {

    private final WPIExtension wpi;

    public WPIExtension getWpi() {
        return wpi;
    }

    @Inject
    public WPISimDepsExtension(WPIExtension wpi) {
        this.wpi = wpi;
    }

    public List<String> driverstation(String platform, boolean debug) {
        return List.of("edu.wpi.first.halsim:halsim_ds_socket:" + wpi.getWpilibVersion() + ":" + platform + (debug ? "debug" : "") + "@zip");
    }

    public List<String> gui(String platform, boolean debug) {
        return List.of("edu.wpi.first.halsim:halsim_gui:" + wpi.getWpilibVersion() + ":" + platform + (debug ? "debug" : "") + "@zip");
    }

    public List<String> ws_server(String platform, boolean debug) {
        return List.of("edu.wpi.first.halsim:halsim_ws_server:" + wpi.getWpilibVersion() + ":" + platform + (debug ? "debug" : "") + "@zip");
    }

    public List<String> ws_client(String platform, boolean debug) {
        return List.of("edu.wpi.first.halsim:halsim_ws_client:" + wpi.getWpilibVersion() + ":" + platform + (debug ? "debug" : "") + "@zip");
    }
}
