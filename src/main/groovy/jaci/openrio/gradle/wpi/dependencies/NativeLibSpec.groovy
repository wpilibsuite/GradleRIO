package jaci.openrio.gradle.wpi.dependencies

import org.gradle.api.Named
import org.gradle.model.Managed
import org.gradle.model.ModelMap

// This will auto-generate the nativeLibs and nativeZip dependencies as needed
// May need to auto-crawl zip

// TODO: Make generic for platform
@Managed
interface NativeLibSpec extends Named {
    void setHeaders(File dir)
    File getHeaders()

    void setStatic(File dir_or_file)
    File getStatic()

    void setShared(File dir_or_file)
    File getShared()

    void setMaven(String dependencyNotation)
    String getMaven()
}

@Managed
interface NativeDependenciesSpec extends ModelMap<NativeLibSpec> { }