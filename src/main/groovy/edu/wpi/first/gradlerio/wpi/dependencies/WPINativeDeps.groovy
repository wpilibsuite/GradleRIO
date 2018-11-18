package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.CombinedNativeLib
import jaci.gradle.nativedeps.NativeDepsSpec
import jaci.gradle.nativedeps.NativeLib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.Mutate
import org.gradle.model.RuleSource

@CompileStatic
class WPINativeDeps implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(WPICommonDeps)
        project.pluginManager.apply(ComponentModelBasePlugin)
    }

    static class WPIDepRules extends RuleSource {

        // Note that throughout here we assign lib.configuration,
        // which specifies which configuration each library artifact belongs
        // in. We group them according to which library they represent, which
        // speeds up dependency resolution. When one dependency in a configuration
        // is downloaded, all are downloaded. Unused configurations are not downloaded

        @Mutate
        void addWPILibraries(NativeDepsSpec libs, final ExtensionContainer extensionContainer) {
            def wpi = extensionContainer.getByType(WPIExtension)
            def common = { NativeLib lib ->
                lib.targetPlatforms = ['roborio']
                lib.headerDirs = []
                lib.sourceDirs = []
                lib.debugMatchers = ['**/*.pdb', '**/*.so.debug']
                lib.staticMatchers = []
            }

            def nativeclassifier = wpi.nativeClassifier

            def createWpiLibrary = { String name, String mavenBase, String libname, boolean supportNative ->
                ['debug', ''].each { buildType->
                    String libnameExt = libname
                    if (buildType.contains('debug')) {
                        libnameExt += 'd'
                    }
                    def cfgName = "native_${name}${buildType}"
                    libs.create("${name}_headers${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.libraryName = "${name}_headers"
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        lib.headerDirs << ''
                        lib.maven = "${mavenBase}:headers@zip"
                        lib.configuration = cfgName
                        null
                    }

                    libs.create("${name}_athena${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.libraryName = "${name}_binaries"
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        lib.sharedMatchers = ["**/lib${libname}d.so".toString()]
                        lib.dynamicMatchers = lib.sharedMatchers
                        lib.maven = "${mavenBase}:linuxathenadebug@zip"
                        lib.configuration = cfgName
                        null
                    }

                    libs.create("${name}_sources${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.libraryName = "${name}_sources"
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        lib.sourceDirs << ''
                        lib.maven = "${mavenBase}:sources@zip"
                        lib.configuration = cfgName
                        null
                    }

                    if (supportNative && nativeclassifier != null) {
                        libs.create("${name}_native${buildType}", NativeLib) { NativeLib lib ->
                            common(lib)
                            lib.libraryName = "${name}_binaries"
                            if (buildType.contains('debug')) {
                                lib.buildType = 'debug'
                            } else {
                                lib.buildType = 'release'
                            }
                            lib.targetPlatforms = ['desktop']
                            lib.sharedMatchers = ["**/shared/*${libnameExt}*.lib".toString(), "**/shared/*${libnameExt}*.so".toString(), "**/shared/*${libnameExt}*.dylib".toString()]

                            lib.dynamicMatchers = lib.sharedMatchers + "**/shared/${libnameExt}*.dll".toString()
                            lib.maven = "${mavenBase}:${nativeclassifier}${buildType}@zip"
                            lib.configuration = "${cfgName}_desktop"
                            null
                        }
                    }
                }

                libs.create("${name}", CombinedNativeLib) { CombinedNativeLib lib ->
                    lib.libs << "${name}_binaries".toString() << "${name}_headers".toString() << "${name}_sources".toString()
                    lib.buildTypes = ['debug', 'release']
                    lib.targetPlatforms = ['roborio']
                    if (supportNative)
                        lib.targetPlatforms << 'desktop'
                    null
                }
            }

            def createWpiStaticLibrary = { String name, String mavenBase, String libname, boolean supportNative ->
                ['debug', ''].each { buildType->
                    String libnameExt = libname
                    if (buildType.contains('debug')) {
                        libnameExt += 'd'
                    }
                    def cfgName = "native_${name}${buildType}"
                    libs.create("${name}_headers${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        lib.libraryName = "${name}_headers"
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        lib.headerDirs << ''
                        lib.maven = "${mavenBase}:headers@zip"
                        lib.configuration = cfgName
                        null
                    }

                    libs.create("${name}_athena${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        lib.libraryName = "${name}_binaries"
                        lib.staticMatchers = ["**/static/*${libname}d.a".toString()]
                        lib.maven = "${mavenBase}:linuxathenastaticdebug@zip"
                        lib.configuration = cfgName
                        null
                    }

                    libs.create("${name}_sources${buildType}", NativeLib) { NativeLib lib ->
                        common(lib)
                        if (buildType.contains('debug')) {
                            lib.buildType = 'debug'
                        } else {
                            lib.buildType = 'release'
                        }
                        lib.libraryName = "${name}_sources"
                        if (supportNative)
                            lib.targetPlatforms << 'desktop'
                        lib.sourceDirs << ''
                        lib.maven = "${mavenBase}:sources@zip"
                        lib.configuration = cfgName
                        null
                    }

                    if (supportNative && nativeclassifier != null) {
                        libs.create("${name}_native${buildType}", NativeLib) { NativeLib lib ->
                            common(lib)
                            if (buildType.contains('debug')) {
                                lib.buildType = 'debug'
                            } else {
                                lib.buildType = 'release'
                            }
                            lib.libraryName = "${name}_binaries"
                            lib.targetPlatforms = ['desktop']
                            lib.staticMatchers = ["**/static/*${libnameExt}.lib".toString(), "**/static/*${libnameExt}.a".toString()]
                            lib.maven = "${mavenBase}:${nativeclassifier}static${buildType}@zip"
                            lib.configuration = "${cfgName}_desktop"
                            null
                        }
                    }
                }

                libs.create("${name}", CombinedNativeLib) { CombinedNativeLib lib ->
                    lib.libs << "${name}_binaries".toString() << "${name}_headers".toString() << "${name}_sources".toString()
                    lib.buildTypes = ['debug', 'release']
                    lib.targetPlatforms = ['roborio']
                    if (supportNative)
                        lib.targetPlatforms << 'desktop'
                    null
                }
            }

            // WPILIB C
            createWpiLibrary('wpilibc', "edu.wpi.first.wpilibc:wpilibc-cpp:${wpi.wpilibVersion}", 'wpilibc', true)

            // HAL
            createWpiLibrary('hal', "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}", 'wpiHal', true)

            // WPILIB C Static
            createWpiStaticLibrary('wpilibc_static', "edu.wpi.first.wpilibc:wpilibc-cpp:${wpi.wpilibVersion}", 'wpilibc', true)

            // HAL Static
            createWpiStaticLibrary('hal_static', "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}", 'wpiHal', true)


            // NI LIBS
            libs.create('ni_chipobject_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildTypes = ['debug', 'release']
                lib.sharedMatchers = ['**/*.so*']
                lib.dynamicMatchers = []    // NI Libs are not deployed to RIO
                lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:linuxathena@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_chipobject_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildTypes = ['debug', 'release']
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:chipobject:${wpi.niLibrariesVersion}:headers@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_netcomm_binaries', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildTypes = ['debug', 'release']
                lib.sharedMatchers = ['**/*.so*']
                lib.dynamicMatchers = []    // NI Libs are not deployed to RIO
                lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:linuxathena@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_netcomm_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildTypes = ['debug', 'release']
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.ni-libraries:netcomm:${wpi.niLibrariesVersion}:headers@zip"
                lib.configuration = 'native_ni_libraries'
                null
            }

            libs.create('ni_libraries', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'ni_chipobject_binaries' << 'ni_chipobject_headers' << 'ni_netcomm_binaries' << 'ni_netcomm_headers'
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatform = 'roborio'
                null
            }


            // WPIUTIL
            createWpiLibrary('wpiutil', "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}", 'wpiutil', true)

            // NTCORE
            createWpiLibrary('ntcore', "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}", 'ntcore', true)

            // CSCORE
            createWpiLibrary('cscore', "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}", 'cscore', true)

            // CAMERASERVER
            createWpiLibrary('cameraserver', "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}", 'cameraserver', true)


            // WPIUTIL
            createWpiStaticLibrary('wpiutil_static', "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}", 'wpiutil', true)

            // NTCORE
            createWpiStaticLibrary('ntcore_static', "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}", 'ntcore', true)

            // CSCORE
            createWpiStaticLibrary('cscore_static', "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}", 'cscore', true)

            // CAMERASERVER
            createWpiStaticLibrary('cameraserver_static', "edu.wpi.first.cameraserver:cameraserver-cpp:${wpi.wpilibVersion}", 'cameraserver', true)

            // GTEST
            createWpiStaticLibrary('googletest', "edu.wpi.first.thirdparty.frc${wpi.wpilibYear}:googletest:${wpi.googleTestVersion}", 'googletest', true)

            // OPENCV
            libs.create('opencv_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'release'
                lib.targetPlatforms << 'desktop'
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
                lib.configuration = 'native_opencv'
                null
            }

            libs.create('opencv_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'release'
                lib.libraryName = 'opencv_binaries'
                lib.dynamicMatchers = ['**/shared/libopencv*.so.*']
                lib.sharedMatchers = ['**/shared/libopencv*.so.*']
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathena@zip"
                lib.configuration = 'native_opencv'
                null
            }

            libs.create('opencvdebug_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'debug'
                lib.libraryName = 'opencv_headers'
                lib.targetPlatforms << 'desktop'
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
                lib.configuration = 'nativedebug_opencv'
                null
            }

            libs.create('opencvdebug_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'debug'
                lib.libraryName = 'opencv_binaries'
                lib.dynamicMatchers = ['**/shared/libopencv*.so.*']
                lib.sharedMatchers = ['**/shared/libopencv*.so.*']
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathenadebug@zip"
                lib.configuration = 'nativedebug_opencv'
                null
            }

            if (nativeclassifier != null) {
                libs.create('opencv_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.buildType = 'release'
                    lib.libraryName = 'opencv_binaries'
                    lib.targetPlatforms = ['desktop']

                    // Need special windows matchers because archive has bad file
                    // Need to fix upstream, but no time before beta
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
                        '**/shared/opencv_videostab343*.lib',
                    ]

                    // The mac matcher is weird because we want to match libopencv_core.3.4.dylib
                    // but not libopencv_java343.dylib. The java library cannot be linked as of 2019 libs.
                    lib.sharedMatchers = ['**/shared/*opencv*.so.*', '**/shared/*opencv*.*.dylib'] + windowsMatchers
                    lib.dynamicMatchers = lib.sharedMatchers + '**/shared/*opencv*.dll'
                    lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}@zip"
                    lib.configuration = 'native_opencv_desktop'
                    null
                }

                libs.create('opencvdebug_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.buildType = 'debug'
                    lib.libraryName = 'opencv_binaries'
                    lib.targetPlatforms = ['desktop']

                    // Need special windows matchers because archive has bad file
                    // Need to fix upstream, but no time before beta
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
                        '**/shared/opencv_videostab343*.lib',
                    ]

                    // The mac matcher is weird because we want to match libopencv_core.3.4.dylib
                    // but not libopencv_java343.dylib. The java library cannot be linked as of 2019 libs.
                    lib.sharedMatchers = ['**/shared/*opencv*.so.*', '**/shared/*opencv*.*.dylib'] + windowsMatchers
                    lib.dynamicMatchers = lib.sharedMatchers + '**/shared/*opencv*.dll'
                    lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}debug@zip"
                    lib.configuration = 'nativedebug_opencv_desktop'
                    null
                }
            }

            libs.create('opencv', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'opencv_binaries' << 'opencv_headers'
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = ['roborio', 'desktop']
                null
            }

            // OPENCV Static
            libs.create('opencv_static_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'release'
                lib.libraryName = 'opencv_static_headers'
                lib.targetPlatforms << 'desktop'
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
                lib.configuration = 'native_opencv'
                null
            }

            libs.create('opencv_static_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'release'
                lib.libraryName = 'opencv_static_binaries'
                lib.staticMatchers = ['**/static/libopencv*.a']
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathenastatic@zip"
                lib.configuration = 'native_opencv'
                null
            }

            libs.create('opencvdebug_static_headers', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'debug'
                lib.libraryName = 'opencv_static_headers'
                lib.targetPlatforms << 'desktop'
                lib.headerDirs << ''
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:headers@zip"
                lib.configuration = 'nativedebug_opencv'
                null
            }

            libs.create('opencvdebug_static_athena', NativeLib) { NativeLib lib ->
                common(lib)
                lib.buildType = 'debug'
                lib.libraryName = 'opencv_static_binaries'
                lib.staticMatchers = ['**/static/libopencv*.a']
                lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:linuxathenastaticdebug@zip"
                lib.configuration = 'native_opencv'
                null
            }

            if (nativeclassifier != null) {
                libs.create('opencv_static_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.buildType = 'release'
                    lib.libraryName = 'opencv_static_binaries'
                    lib.targetPlatforms = ['desktop']

                    lib.staticMatchers = ['**/static/*opencv*.a', '**/static/*opencv*.lib']
                    lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}static@zip"
                    lib.configuration = 'native_opencv_desktop'
                    null
                }

                libs.create('opencvdebug_static_native', NativeLib) { NativeLib lib ->
                    common(lib)
                    lib.buildType = 'debug'
                    lib.libraryName = 'opencv_static_binaries'
                    lib.targetPlatforms = ['desktop']

                    lib.staticMatchers = ['**/static/*opencv*.a', '**/static/*opencv*.lib']
                    lib.maven = "edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${nativeclassifier}staticdebug@zip"
                    lib.configuration = 'nativedebug_opencv_desktop'
                    null
                }
            }

            libs.create('opencv_static', CombinedNativeLib) { CombinedNativeLib lib ->
                lib.libs << 'opencv_static_binaries' << 'opencv_static_headers'
                lib.buildTypes = ['debug', 'release']
                lib.targetPlatforms = ['roborio', 'desktop']
                null
            }

            // MASTER WPILIB COMBINED LIB

            libs.create('wpilib', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "cameraserver" << "opencv" << "ni_libraries"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilib_static', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "wpilibc_static" << "hal_static" << "cameraserver_static" << "ntcore_static" << "cscore_static" << "wpiutil_static" << "opencv_static" << "ni_libraries"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilibjni', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libs << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv" << "ni_libraries"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['roborio']
                null
            }

            libs.create('wpilib_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilib'
                clib.libs << "wpilibc" << "hal" << "wpiutil" << "ntcore" << "cscore" << "cameraserver" << "opencv"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['desktop']
                null
            }

            libs.create('wpilib_static_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilib_static'
                clib.libs << "wpilibc_static" << "hal_static" << "cameraserver_static" << "ntcore_static" << "cscore_static" << "wpiutil_static" << "opencv_static"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['desktop']
                null
            }

            libs.create('wpilibjni_sim', CombinedNativeLib) { CombinedNativeLib clib ->
                clib.libraryName = 'wpilibjni'
                clib.libs << "hal" << "wpiutil" << "ntcore" << "cscore" << "opencv"
                clib.buildTypes = ['debug', 'release']
                clib.targetPlatforms = ['desktop']
                null
            }
        }
    }
}
