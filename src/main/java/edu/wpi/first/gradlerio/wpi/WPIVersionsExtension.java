package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.provider.Property;

public abstract class WPIVersionsExtension {

    private static final String wpilibVersion = "2022.1.1-beta-3";
    private static final String niLibrariesVersion = "2022.2.3";
    private static final String opencvVersion = "4.5.2-1";
    private static final String imguiVersion = "1.82-1";
    private static final String ejmlVersion = "0.41";
    private static final String jacksonVersion = "2.10.0";
    private static final String wpimathVersion = "2022.1.1-beta-3";
    private static final String googleTestVersion = "1.9.0-5-437e100-1";

    private static final String smartDashboardVersion = "2022.1.1-beta-3";
    private static final String shuffleboardVersion = "2022.1.1-beta-3";
    private static final String outlineViewerVersion = "2022.1.1-beta-3";
    private static final String robotBuilderVersion = "2022.1.1-beta-3";
    private static final String robotBuilderOldVersion = "2022.1.1-beta-3";
    private static final String pathWeaverVersion = "2022.1.1-beta-3";
    private static final String glassVersion = "2022.1.1-beta-3";
    private static final String sysIdVersion = "2022.1.1-beta-3";
    private static final String roboRIOTeamNumberSetterVersion = "2022.1.1-beta-3";


    public abstract Property<String> getWpilibVersion();
    public abstract Property<String> getNiLibrariesVersion();
    public abstract Property<String> getOpencvVersion();
    public abstract Property<String> getGoogleTestVersion();
    public abstract Property<String> getImguiVersion();
    public abstract Property<String> getWpimathVersion();

    public abstract Property<String> getEjmlVersion();
    public abstract Property<String> getJacksonVersion();
    public abstract Property<String> getSmartDashboardVersion();
    public abstract Property<String> getShuffleboardVersion();
    public abstract Property<String> getOutlineViewerVersion();
    public abstract Property<String> getRobotBuilderVersion();
    public abstract Property<String> getRobotBuilderOldVersion();
    public abstract Property<String> getPathWeaverVersion();
    public abstract Property<String> getGlassVersion();
    public abstract Property<String> getSysIdVersion();
    public abstract Property<String> getRoboRIOTeamNumberSetterVersion();

    @Inject
    public WPIVersionsExtension() {
        getWpilibVersion().convention(wpilibVersion);
        getNiLibrariesVersion().convention(niLibrariesVersion);
        getOpencvVersion().convention(opencvVersion);
        getGoogleTestVersion().convention(googleTestVersion);
        getImguiVersion().convention(imguiVersion);
        getWpimathVersion().convention(wpimathVersion);

        getEjmlVersion().convention(ejmlVersion);
        getJacksonVersion().convention(jacksonVersion);
        getSmartDashboardVersion().convention(smartDashboardVersion);
        getShuffleboardVersion().convention(shuffleboardVersion);
        getOutlineViewerVersion().convention(outlineViewerVersion);
        getRobotBuilderVersion().convention(robotBuilderVersion);
        getRobotBuilderOldVersion().convention(robotBuilderOldVersion);
        getPathWeaverVersion().convention(pathWeaverVersion);
        getGlassVersion().convention(glassVersion);
        getSysIdVersion().convention(sysIdVersion);
        getRoboRIOTeamNumberSetterVersion().convention(roboRIOTeamNumberSetterVersion);
    }

}
