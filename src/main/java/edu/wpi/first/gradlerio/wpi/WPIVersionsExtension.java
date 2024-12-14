package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.provider.Property;

public abstract class WPIVersionsExtension {

    private static final String wpilibVersion = "2025.1.1-beta-2";
    private static final String niLibrariesVersion = "2025.0.0";
    private static final String opencvVersion = "4.8.0-4";
    private static final String imguiVersion = "1.89.9-1";
    private static final String ejmlVersion = "0.43.1";
    private static final String jacksonVersion = "2.15.2";
    private static final String quickbufVersion = "1.3.3";
    private static final String wpimathVersion = "2025.1.1-beta-2";

    private static final String smartDashboardVersion = "2025.1.1-beta-2";
    private static final String shuffleboardVersion = "2025.1.1-beta-2";
    private static final String outlineViewerVersion = "2025.1.1-beta-2";
    private static final String robotBuilderVersion = "2025.1.1-beta-2";
    private static final String pathWeaverVersion = "2025.1.1-beta-2";
    private static final String glassVersion = "2025.1.1-beta-2";
    private static final String sysIdVersion = "2025.1.1-beta-2";
    private static final String roboRIOTeamNumberSetterVersion = "2025.1.1-beta-2";
    private static final String dataLogToolVersion = "2025.1.1-beta-2";


    public abstract Property<String> getWpilibVersion();
    public abstract Property<String> getNiLibrariesVersion();
    public abstract Property<String> getOpencvVersion();
    public abstract Property<String> getImguiVersion();
    public abstract Property<String> getWpimathVersion();

    public abstract Property<String> getEjmlVersion();
    public abstract Property<String> getJacksonVersion();
    public abstract Property<String> getQuickbufVersion();
    public abstract Property<String> getSmartDashboardVersion();
    public abstract Property<String> getShuffleboardVersion();
    public abstract Property<String> getOutlineViewerVersion();
    public abstract Property<String> getRobotBuilderVersion();
    public abstract Property<String> getPathWeaverVersion();
    public abstract Property<String> getGlassVersion();
    public abstract Property<String> getSysIdVersion();
    public abstract Property<String> getRoboRIOTeamNumberSetterVersion();
    public abstract Property<String> getDataLogToolVersion();

    @Inject
    public WPIVersionsExtension() {
        getWpilibVersion().convention(wpilibVersion);
        getNiLibrariesVersion().convention(niLibrariesVersion);
        getOpencvVersion().convention(opencvVersion);
        getImguiVersion().convention(imguiVersion);
        getWpimathVersion().convention(wpimathVersion);

        getEjmlVersion().convention(ejmlVersion);
        getJacksonVersion().convention(jacksonVersion);
        getQuickbufVersion().convention(quickbufVersion);
        getSmartDashboardVersion().convention(smartDashboardVersion);
        getShuffleboardVersion().convention(shuffleboardVersion);
        getOutlineViewerVersion().convention(outlineViewerVersion);
        getRobotBuilderVersion().convention(robotBuilderVersion);
        getPathWeaverVersion().convention(pathWeaverVersion);
        getGlassVersion().convention(glassVersion);
        getSysIdVersion().convention(sysIdVersion);
        getRoboRIOTeamNumberSetterVersion().convention(roboRIOTeamNumberSetterVersion);
        getDataLogToolVersion().convention(dataLogToolVersion);
    }

}
