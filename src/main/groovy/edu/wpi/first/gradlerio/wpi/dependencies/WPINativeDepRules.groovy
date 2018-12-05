package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.toolchain.NativePlatforms
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

@CompileStatic
class WPINativeDepRules extends RuleSource {

    // Note that throughout here we assign lib.configuration,
    // which specifies which configuration each library artifact belongs
    // in. We group them according to which library they represent, which
    // speeds up dependency resolution. When one dependency in a configuration
    // is downloaded, all are downloaded. Unused configurations are not downloaded

    @Mutate
    void addAllNativeDeps(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
        def wpi = extensionContainer.getByType(WPIExtension)
        addWPILibraries(libs, wpi)
        addThirdPartyLibraries(libs, wpi)
    }

    private static void common(NativeLib lib) {
        lib.targetPlatforms = []
        lib.headerDirs = []
        lib.sourceDirs = []
        lib.debugMatchers = ['**/*.pdb', '**/*.so.debug']
        lib.staticMatchers = []
    }

    private static void matchersShared(NativeLib lib, String libname, boolean desktop) {
        if (desktop) {
            lib.sharedMatchers = [
                "**/shared/*${libname}*.lib".toString(),
                "**/shared/*${libname}*.so".toString(),
                "**/shared/*${libname}*.dylib".toString()
            ]
            lib.dynamicMatchers = lib.sharedMatchers + "**/shared/*${libname}*.dll".toString()
        } else {
            lib.sharedMatchers = [
                "**/*${libname}*.so.*".toString()
            ]
            lib.dynamicMatchers = lib.sharedMatchers
        }
    }

    private static void matchersStatic(NativeLib lib, String libname, boolean desktop) {
        if (desktop) {
            lib.staticMatchers = [
                "**/static/*${libname}*.a".toString()
            ]
        } else {
            lib.staticMatchers = [
                "**/static/*${libname}*.lib".toString(),
                "**/static/*${libname}*.a".toString()
            ]
        }
    }

    private static void createWpiLibrary(NativeDepsSpec libs, String name, String mavenBase, String libName, boolean supportDesktop, boolean supportRaspbian, boolean shared) {
        ['debug', ''].each { String buildKind ->
            String buildType    = buildKind.contains('debug') ? 'debug' : 'release'
            String libSuffix    = buildKind.contains('debug') ? 'd' : ''
            String config       = "native_${name}${buildKind}".toString()
            String linkSuff     = shared ? '' : 'static'

            libs.create("${name}_headers${buildKind}".toString(), NativeLib, { NativeLib lib ->
                common(lib)
                lib.targetPlatforms << NativePlatforms.roborio
                if (supportDesktop)
                    lib.targetPlatforms << NativePlatforms.desktop
                if (supportRaspbian)
                    lib.targetPlatforms << NativePlatforms.raspbian
                lib.libraryName = "${name}_headers"
                lib.buildType = buildType
                lib.headerDirs.add('')
                lib.maven = "${mavenBase}:headers@zip"
                lib.configuration = config
            } as Action<? extends NativeLib>)

            libs.create("${name}_athena${buildKind}".toString(), NativeLib, { NativeLib lib ->
                common(lib)
                if (shared)
                    matchersShared(lib, libName + 'd', false)
                else
                    matchersStatic(lib, libName + 'd', false)
                lib.targetPlatforms << NativePlatforms.roborio
                lib.libraryName = "${name}_binaries"
                lib.buildType = buildType
                lib.maven = "${mavenBase}:${NativePlatforms.roborio}${linkSuff}debug@zip"
                lib.configuration = config
            } as Action<? extends NativeLib>)

            if (supportDesktop) {
                libs.create("${name}_native${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    if (shared)
                        matchersShared(lib, libName + libSuffix, true)
                    else
                        matchersStatic(lib, libName + libSuffix, true)
                    lib.targetPlatforms << NativePlatforms.desktop
                    lib.libraryName = "${name}_binaries"
                    lib.buildType = buildType
                    lib.maven = "${mavenBase}:${NativePlatforms.desktop}${linkSuff}${buildKind}@zip"
                    lib.configuration = "${config}_desktop"
                } as Action<? extends NativeLib>)
            }

            if (supportRaspbian) {
                libs.create("${name}_raspbian${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    if (shared)
                        matchersShared(lib, libName + libSuffix, false)
                    else
                        matchersStatic(lib, libName + libSuffix, false)
                    lib.targetPlatforms << NativePlatforms.raspbian
                    lib.libraryName = "${name}_binaries"
                    lib.buildType = buildType
                    lib.maven = "${mavenBase}:${NativePlatforms.raspbian}${linkSuff}${buildKind}@zip"
                    lib.configuration = "${config}_raspbian"
                } as Action<? extends NativeLib>)
            }

            libs.create("${name}_sources${buildKind}".toString(), NativeLib, { NativeLib lib ->
                common(lib)
                lib.targetPlatforms << NativePlatforms.roborio
                if (supportDesktop)
                    lib.targetPlatforms << NativePlatforms.desktop
                if (supportRaspbian)
                    lib.targetPlatforms << NativePlatforms.raspbian
                lib.libraryName = "${name}_sources"
                lib.buildType = buildType
                lib.sourceDirs << ''
                lib.maven = "${mavenBase}:sources@zip"
                lib.configuration = config
            } as Action<? extends NativeLib>)
        }

        libs.create(name, CombinedNativeLib, { CombinedNativeLib lib ->
            lib.libs << "${name}_binaries".toString() << "${name}_headers".toString() << "${name}_sources".toString()
            lib.buildTypes = ['debug', 'release']
            lib.targetPlatforms = [NativePlatforms.roborio]
            if (supportDesktop)
                lib.targetPlatforms << NativePlatforms.desktop
            if (supportRaspbian)
                lib.targetPlatforms << NativePlatforms.raspbian
        } as Action<? extends CombinedNativeLib>)
    }

    private static void addWPILibraries(NativeDepsSpec libs, final WPIExtension wpi) {
        for (boolean shared in [true, false]) {
            def suf = shared ? '' : '_static'

            createWpiLibrary(libs, 'wpilibc' + suf, "edu.wpi.first.wpilibc:wpilibc-cpp:${wpi.wpilibVersion}", 'wpilibc', true, true, shared)
            createWpiLibrary(libs, 'hal' + suf, "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}", 'wpiHal', true, true, shared)
            createWpiLibrary(libs, 'wpiutil' + suf, "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}", 'wpiutil', true, true, shared)
            createWpiLibrary(libs, 'ntcore' + suf, "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}", 'ntcore', true, true, shared)
            createWpiLibrary(libs, 'cscore' + suf, "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}", 'cscore', true, true, shared)
            createWpiLibrary(libs, 'cameraserver' + suf, "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}", 'cameraserver', true, true, shared)

            libs.create('wpilib_common' + suf, CombinedNativeLib, { CombinedNativeLib lib ->
                lib.libs << 'wpilibc' + suf << 'hal' + suf << 'wpiutil' + suf << 'ntcore' + suf << 'cscore' + suf << 'cameraserver' + suf << 'opencv' + suf
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = [wpi.platforms.roborio, wpi.platforms.desktop, wpi.platforms.raspbian]
            } as Action<? extends CombinedNativeLib>)


            libs.create('wpilib' + suf, CombinedNativeLib, { CombinedNativeLib lib ->
                lib.libs << 'wpilib_common' + suf << 'ni_libraries'
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = [wpi.platforms.roborio]
            } as Action<? extends CombinedNativeLib>)

            libs.create('wpilib' + suf + '_sim', CombinedNativeLib, { CombinedNativeLib lib ->
                lib.libraryName = 'wpilib' + suf
                lib.libs << 'wpilib_common' + suf
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = [wpi.platforms.desktop, wpi.platforms.raspbian]
            } as Action<? extends CombinedNativeLib>)
        }

        // NI Libraries
        libs.create('ni_chipobject_binaries', NativeLib, { NativeLib lib ->
            common(lib)
            lib.targetPlatforms << wpi.platforms.roborio
            lib.buildTypes = ['debug', 'release']
            lib.sharedMatchers = ['**/*.so*']
            lib.dynamicMatchers = []    // NI Libs are not deployed to RRIO
            lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:${wpi.platforms.roborio}@zip"
            lib.configuration = 'native_ni'
        } as Action<? extends NativeLib>)

        libs.create('ni_chipobject_headers', NativeLib, { NativeLib lib ->
            common(lib)
            lib.targetPlatforms << wpi.platforms.roborio
            lib.buildTypes = ['debug', 'release']
            lib.headerDirs << ''
            lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:headers@zip"
            lib.configuration = 'native_ni'
        } as Action<? extends NativeLib>)

        libs.create('ni_netcomm_binaries', NativeLib, { NativeLib lib ->
            common(lib)
            lib.targetPlatforms << wpi.platforms.roborio
            lib.buildTypes = ['debug', 'release']
            lib.sharedMatchers = ['**/*.so*']
            lib.dynamicMatchers = []    // NI Libs are not deployed to RRIO
            lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:${wpi.platforms.roborio}@zip"
            lib.configuration = 'native_ni'
        } as Action<? extends NativeLib>)

        libs.create('ni_netcomm_headers', NativeLib, { NativeLib lib ->
            common(lib)
            lib.targetPlatforms << wpi.platforms.roborio
            lib.buildTypes = ['debug', 'release']
            lib.headerDirs << ''
            lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:headers@zip"
            lib.configuration = 'native_ni'
        } as Action<? extends NativeLib>)

        // Non-static combined libs
        libs.create('ni_libraries', CombinedNativeLib, { CombinedNativeLib lib ->
            lib.libs << 'ni_chipobject_binaries' << 'ni_chipobject_headers' << 'ni_netcomm_binaries' << 'ni_netcomm_headers'
            lib.buildTypes = ['debug', 'release']
            lib.targetPlatforms = [wpi.platforms.roborio]
        } as Action<? extends CombinedNativeLib>)

        libs.create('wpilibjni_common', CombinedNativeLib, { CombinedNativeLib lib ->
            lib.libs << 'hal' << 'wpiutil' << 'ntcore' << 'cscore' << 'opencv'
            lib.buildTypes = ['debug', 'release']
            lib.targetPlatforms = [wpi.platforms.roborio, wpi.platforms.desktop, wpi.platforms.raspbian]
        } as Action<? extends CombinedNativeLib>)

        libs.create('wpilibjni', CombinedNativeLib, { CombinedNativeLib lib ->
            lib.libs << 'wpilibjni_common' << 'ni_libraries'
            lib.buildTypes = ['debug', 'release']
            lib.targetPlatforms = [wpi.platforms.roborio]
        } as Action<? extends CombinedNativeLib>)

        libs.create('wpilibjni_sim', CombinedNativeLib, { CombinedNativeLib lib ->
            lib.libs << 'wpilibjni_common'
            lib.buildTypes = ['debug', 'release']
            lib.targetPlatforms = [wpi.platforms.desktop, wpi.platforms.raspbian]
        } as Action<? extends CombinedNativeLib>)
    }

    private static void addThirdPartyLibraries(NativeDepsSpec libs, final WPIExtension wpi) {
        createWpiLibrary(libs, 'googletest', "edu.wpi.first.thirdparty.frc${wpi.wpilibYear}:googletest:${wpi.googleTestVersion}", 'googletest', true, true, false)

        // OpenCV is special
        for (boolean shared in [true, false]) {
            def mavenRoot = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}"
            def suf = shared ? '' : '_static'
            ['debug', ''].each { String buildKind ->
                String buildType    = buildKind.contains('debug') ? 'debug' : 'release'
                String config       = "native_opencv${buildKind}".toString()
                String linkSuff     = shared ? '' : 'static'
                boolean isShared    = shared   // Action calls are deferred, so the value of shared can be broken since

                def windowsMatchers = [
                    '**/shared/opencv_calib3d343*.lib',
                    '**/shared/opencv_core343*.lib',
                    '**/shared/opencv_dnn343*.lib',
                    '**/shared/opencv_features2d343*.lib',
                    '**/shared/opencv_flann343*.lib',
                    '**/shared/opencv_highgui343*.lib',
                    '**/shared/opencv_imgcodecs343*.lib',
                    '**/shared/opencv_imgproc343*.lib',
                    '**/shared/opencv_ml343*.lib',
                    '**/shared/opencv_objdetect343*.lib',
                    '**/shared/opencv_photo343*.lib',
                    '**/shared/opencv_shape343*.lib',
                    '**/shared/opencv_stitching343*.lib',
                    '**/shared/opencv_superres343*.lib',
                    '**/shared/opencv_video343*.lib',
                    '**/shared/opencv_videoio343*.lib',
                    '**/shared/opencv_videostab343*.lib'
                ] as List<String>

                libs.create("opencv${suf}_headers${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    lib.targetPlatforms = [NativePlatforms.roborio, NativePlatforms.desktop, wpi.platforms.raspbian]
                    lib.libraryName = "opencv${suf}_headers"
                    lib.buildType = buildType
                    lib.headerDirs.add('')
                    lib.maven = "${mavenRoot}:headers@zip"
                    lib.configuration = config
                } as Action<? extends NativeLib>)

                libs.create("opencv${suf}_athena${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    if (isShared) {
                        lib.sharedMatchers = ['**/shared/libopencv*.so.*']
                        lib.dynamicMatchers = lib.sharedMatchers
                    } else {
                        matchersStatic(lib, 'opencv', false)
                    }
                    lib.targetPlatforms << NativePlatforms.roborio
                    lib.libraryName = "opencv${suf}_binaries"
                    lib.buildType = buildType
                    lib.maven = "${mavenRoot}:${NativePlatforms.roborio}${linkSuff}debug@zip"
                    lib.configuration = config
                } as Action<? extends NativeLib>)

                libs.create("opencv${suf}_native${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    if (isShared) {
                        lib.sharedMatchers = ['**/shared/*opencv*.so.*', '**/shared/*opencv*.*.dylib'] + windowsMatchers
                        lib.dynamicMatchers = lib.sharedMatchers + '**/shared/*opencv*.dll'
                    } else {
                        matchersStatic(lib, 'opencv', true)
                    }
                    lib.targetPlatforms << NativePlatforms.desktop
                    lib.libraryName = "opencv${suf}_binaries"
                    lib.buildType = buildType
                    lib.maven = "${mavenRoot}:${NativePlatforms.desktop}${linkSuff}${buildKind}@zip"
                    lib.configuration = "${config}_desktop"
                } as Action<? extends NativeLib>)

                libs.create("opencv${suf}_raspbian${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    if (isShared) {
                        lib.sharedMatchers = ['**/shared/*opencv*.so.*']
                    } else {
                        matchersStatic(lib, 'opencv', false)
                    }
                    lib.targetPlatforms << NativePlatforms.raspbian
                    lib.libraryName = "opencv${suf}_binaries"
                    lib.buildType = buildType
                    lib.maven = "${mavenRoot}:${NativePlatforms.raspbian}${linkSuff}${buildKind}@zip"
                    lib.configuration = "${config}_raspbian"
                } as Action<? extends NativeLib>)

                libs.create("opencv${suf}_sources${buildKind}".toString(), NativeLib, { NativeLib lib ->
                    common(lib)
                    lib.targetPlatforms = [NativePlatforms.roborio, NativePlatforms.desktop, NativePlatforms.raspbian]
                    lib.libraryName = "opencv${suf}_sources"
                    lib.buildType = buildType
                    lib.sourceDirs << ''
                    lib.maven = "${mavenRoot}:sources@zip"
                    lib.configuration = config
                } as Action<? extends NativeLib>)
            }

            libs.create('opencv' + suf, CombinedNativeLib, { CombinedNativeLib lib ->
                lib.libs << "opencv${suf}_binaries".toString() << "opencv${suf}_headers".toString() << "opencv${suf}_sources".toString()
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = [NativePlatforms.roborio, NativePlatforms.desktop, NativePlatforms.raspbian]
            } as Action<? extends CombinedNativeLib>)
        }
    }
}
