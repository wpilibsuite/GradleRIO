package org.wpilib.gradlerio.wpi;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public abstract class WPIVersionsExtension {

    private static final String wpilibVersion = "2027.0.0-alpha-8";
    private static final String avajeVersion = "3.14";
    private static final String ejmlVersion = "0.46.0";
    private static final String opencvVersion = "2027-4.13.0-4";
    private static final String quickbufVersion = "1.4";

    private static final String outlineViewerVersion = "2027.0.0-alpha-8";
    private static final String glassVersion = "2027.0.0-alpha-8";
    private static final String sysIdVersion = "2027.0.0-alpha-8";
    private static final String dataLogToolVersion = "2027.0.0-alpha-8";
    private static final String wpicalToolVersion = "2027.0.0-alpha-8";
    private static final String mrcLibVersion = "2027.1.0-alpha-1-116-g5288562";

    public abstract Property<String> getWpilibVersion();

    public abstract Property<String> getAvajeVersion();
    public abstract Property<String> getEjmlVersion();
    public abstract Property<String> getOpencvVersion();
    public abstract Property<String> getQuickbufVersion();
    public abstract Property<String> getOutlineViewerVersion();
    public abstract Property<String> getGlassVersion();
    public abstract Property<String> getSysIdVersion();
    public abstract Property<String> getDataLogToolVersion();
    public abstract Property<String> getWpicalToolVersion();
    public abstract Property<String> getMrcLibVersion();

    public abstract MapProperty<String, String> getLibraryVersions();

    @Inject
    public WPIVersionsExtension() {
        getWpilibVersion().convention(wpilibVersion);

        getAvajeVersion().convention(avajeVersion);
        getEjmlVersion().convention(ejmlVersion);
        getOpencvVersion().convention(opencvVersion);
        getQuickbufVersion().convention(quickbufVersion);
        getOutlineViewerVersion().convention(outlineViewerVersion);
        getGlassVersion().convention(glassVersion);
        getSysIdVersion().convention(sysIdVersion);
        getDataLogToolVersion().convention(dataLogToolVersion);
        getWpicalToolVersion().convention(wpicalToolVersion);
        getMrcLibVersion().convention(mrcLibVersion);

        getLibraryVersions().convention(Map.of(
            "wpilib", wpilibVersion,
            "wpilib.avaje", avajeVersion,
            "wpilib.ejml", ejmlVersion,
            "wpilib.opencv", opencvVersion,
            "wpilib.quickbuf", quickbufVersion
        ));
    }
}
