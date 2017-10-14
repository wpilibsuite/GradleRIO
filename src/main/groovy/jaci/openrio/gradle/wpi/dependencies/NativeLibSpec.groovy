package jaci.openrio.gradle.wpi.dependencies

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.model.Managed
import org.gradle.model.ModelMap

// This will auto-generate the nativeLibs and nativeZip dependencies as needed
// May need to auto-crawl zip

// TODO: Make generic for platform
@Managed
@CompileStatic
interface NativeLibSpec extends Named {
    void setHeaderDirs(List<String> dirs)
    List<String> getHeaderDirs()

    void setStaticMatchers(List<String> matchers)
    List<String> getStaticMatchers()

    void setSharedMatchers(List<String> matchers)
    List<String> getSharedMatchers()

    void setLibraries(List<String> libraries)
    List<String> getLibraries()

    // Companion libs, i.e. other libraries that this one depends on
    void setCompanions(List<String> companionLibs)
    List<String> getCompanions()

    void setMaven(String dependencyNotation)
    String getMaven()

    void setFile(File dir_or_zip)
    File getFile()

    void setTargetPlatforms(List<String> platforms)
    List<String> getTargetPlatforms()
}

@Managed
@CompileStatic
interface NativeDependenciesSpec extends ModelMap<NativeLibSpec> { }