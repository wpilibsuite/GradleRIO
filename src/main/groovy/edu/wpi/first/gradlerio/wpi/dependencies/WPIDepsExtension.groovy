package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic

@CompileStatic
public class WPIDepsExtension {

    final WPIExtension wpi

    WPIVendorDepsExtension vendor

    WPIDepsExtension(WPIExtension wpi) {
        this.wpi = wpi
        this.vendor = new WPIVendorDepsExtension(this)
    }

    List<String> wpilib() {
        wpilibJni().each {
            wpi.project.dependencies.add("nativeZip", it)
        }
        wpilibDesktopJni().each {
            wpi.project.dependencies.add("nativeDesktopZip", it)
        }
        return wpilibJars()
    }

    List<String> wpilibJars() {
        return ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.thirdparty.frc2019.opencv:opencv-java:${wpi.opencvVersion}".toString(),
                "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}".toString(),
                "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}".toString()]
    }

    List<String> wpilibSource() {
        return ["edu.wpi.first.wpilibj:wpilibj-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.ntcore:ntcore-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.wpiutil:wpiutil-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.thirdparty.frc2019.opencv:opencv-java:${wpi.opencvVersion}:sources".toString(),
                "edu.wpi.first.cscore:cscore-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.cameraserver:cameraserver-java:${wpi.wpilibVersion}:sources".toString(),
                "edu.wpi.first.hal:hal-java:${wpi.wpilibVersion}:sources".toString()]
    }

    List<String> wpilibJni() {
        // Note: we use -cpp artifacts instead of -jni artifacts as the -cpp ones are linked with shared
        // libraries, while the -jni ones are standalone (have static libs embedded).
        return ["edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${wpi.platforms.roborio}debug@zip".toString(),
                "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${wpi.platforms.roborio}debug@zip".toString(),
                "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${wpi.platforms.roborio}debug@zip".toString(),
                "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${wpi.platforms.roborio}debug@zip".toString(),
                "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${wpi.platforms.roborio}debug@zip".toString()]
    }

    List<String> wpilibDesktopJni() {
        def debug = wpi.debugSimJNI ? "debug" : ""

        return ["edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${wpi.platforms.desktop}${debug}@zip".toString(),
                "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}${debug}@zip".toString(),
                "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}${debug}@zip".toString(),
                "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}${debug}@zip".toString(),
                "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${wpi.platforms.desktop}${debug}@zip".toString()]
    }

    // TODO: Need Raspbian JNI Configuration!
    List<String> wpilibRaspbianJni() {
        return ["edu.wpi.first.thirdparty.frc2019.opencv:opencv-cpp:${wpi.opencvVersion}:${wpi.platforms.raspbian}@zip".toString(),
                "edu.wpi.first.hal:hal-cpp:${wpi.wpilibVersion}:${wpi.platforms.raspbian}@zip".toString(),
                "edu.wpi.first.wpiutil:wpiutil-cpp:${wpi.wpilibVersion}:${wpi.platforms.raspbian}@zip".toString(),
                "edu.wpi.first.ntcore:ntcore-cpp:${wpi.wpilibVersion}:${wpi.platforms.raspbian}@zip".toString(),
                "edu.wpi.first.cscore:cscore-cpp:${wpi.wpilibVersion}:${wpi.platforms.raspbian}@zip".toString()]
    }

    class WPISimExtension {
        List<String> print() {
            return ["edu.wpi.first.halsim:halsim-print:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
        }

        List<String> nt_ds() {
            return ["edu.wpi.first.halsim.ds:halsim-ds-nt:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
        }

        List<String> lowfi() {
            return ["edu.wpi.first.halsim:halsim-lowfi:${wpi.wpilibVersion}:${wpi.platforms.desktop}@zip".toString()]
        }
    }

}
