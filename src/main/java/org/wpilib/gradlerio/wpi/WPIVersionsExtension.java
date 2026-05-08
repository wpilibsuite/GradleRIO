package org.wpilib.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.provider.Property;

public abstract class WPIVersionsExtension {

    private static final String wpilibVersion = "2027.0.0-alpha-6";
    private static final String opencvVersion = "2027-4.13.0-3";
    private static final String avajeVersion = "3.11";
    private static final String ejmlVersion = "0.44.0";
    private static final String quickbufVersion = "1.4";

    private static final String outlineViewerVersion = "2027.0.0-alpha-6";
    private static final String glassVersion = "2027.0.0-alpha-6";
    private static final String sysIdVersion = "2027.0.0-alpha-6";
    private static final String dataLogToolVersion = "2027.0.0-alpha-6";
    private static final String wpicalToolVersion = "2027.0.0-alpha-6";
    private static final String processstarterToolVersion = "2027.0.0-alpha-6";

    public abstract Property<String> getWpilibVersion();
    public abstract Property<String> getOpencvVersion();

    public abstract Property<String> getAvajeVersion();
    public abstract Property<String> getEjmlVersion();
    public abstract Property<String> getQuickbufVersion();
    public abstract Property<String> getOutlineViewerVersion();
    public abstract Property<String> getGlassVersion();
    public abstract Property<String> getSysIdVersion();
    public abstract Property<String> getDataLogToolVersion();
    public abstract Property<String> getWpicalToolVersion();
    public abstract Property<String> getProcessstarterToolVersion();

    @Inject
    public WPIVersionsExtension() {
        getWpilibVersion().convention(wpilibVersion);
        getOpencvVersion().convention(opencvVersion);

        getAvajeVersion().convention(avajeVersion);
        getEjmlVersion().convention(ejmlVersion);
        getQuickbufVersion().convention(quickbufVersion);
        getOutlineViewerVersion().convention(outlineViewerVersion);
        getGlassVersion().convention(glassVersion);
        getSysIdVersion().convention(sysIdVersion);
        getDataLogToolVersion().convention(dataLogToolVersion);
        getWpicalToolVersion().convention(wpicalToolVersion);
        getProcessstarterToolVersion().convention(processstarterToolVersion);
    }

}
