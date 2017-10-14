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
    void setHeaderMatchers(List<String> matchers)
    List<String> getHeaderMatchers()

    void setStaticMatchers(List<String> matchers)
    List<String> getStaticMatchers()

    void setSharedMatchers(List<String> matchers)
    List<String> getSharedMatchers()

    void setMaven(String dependencyNotation)
    String getMaven()

    void setFile(File dir_or_file)
    File getFile()
}

@Managed
@CompileStatic
interface NativeDependenciesSpec extends ModelMap<NativeLibSpec> { }