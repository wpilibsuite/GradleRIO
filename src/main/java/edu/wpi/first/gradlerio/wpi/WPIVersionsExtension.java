package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.provider.Property;

public abstract class WPIVersionsExtension {

    private static final String wpilibVersion = "2027.0.0-alpha-3-70-g04fb95a";
    private static final String opencvVersion = "4.10.0-3";
    private static final String imguiVersion = "1.89.9-1";
    private static final String ejmlVersion = "0.43.1";
    private static final String jacksonVersion = "2.15.2";
    private static final String quickbufVersion = "1.3.3";

    private static final String outlineViewerVersion = "2027.0.0-alpha-3-70-g04fb95a";
    private static final String glassVersion = "2027.0.0-alpha-3-70-g04fb95a";
    private static final String sysIdVersion = "2027.0.0-alpha-3-70-g04fb95a";
    private static final String dataLogToolVersion = "2027.0.0-alpha-3-70-g04fb95a";


    public abstract Property<String> getWpilibVersion();
    public abstract Property<String> getOpencvVersion();
    public abstract Property<String> getImguiVersion();

    public abstract Property<String> getEjmlVersion();
    public abstract Property<String> getJacksonVersion();
    public abstract Property<String> getQuickbufVersion();
    public abstract Property<String> getOutlineViewerVersion();
    public abstract Property<String> getGlassVersion();
    public abstract Property<String> getSysIdVersion();
    public abstract Property<String> getRoboRIOTeamNumberSetterVersion();
    public abstract Property<String> getDataLogToolVersion();

    @Inject
    public WPIVersionsExtension() {
        getWpilibVersion().convention(wpilibVersion);
        getOpencvVersion().convention(opencvVersion);
        getImguiVersion().convention(imguiVersion);

        getEjmlVersion().convention(ejmlVersion);
        getJacksonVersion().convention(jacksonVersion);
        getQuickbufVersion().convention(quickbufVersion);
        getOutlineViewerVersion().convention(outlineViewerVersion);
        getGlassVersion().convention(glassVersion);
        getSysIdVersion().convention(sysIdVersion);
        getDataLogToolVersion().convention(dataLogToolVersion);
    }

}
