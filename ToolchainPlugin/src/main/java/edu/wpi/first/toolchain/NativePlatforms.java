package edu.wpi.first.toolchain;

import org.gradle.internal.os.OperatingSystem;

public class NativePlatforms {
    public static final String desktop = desktopOS() + desktopArch();
    public static final String roborio = "linuxathena";
    public static final String raspbian = "linuxraspbian";

    public static String desktopArch() {
        String arch = System.getProperty("os.arch");
        return (arch.equals("amd64") || arch.equals("x86_64")) ? "x86-64" : "x86";
    }

    public static String desktopOS() {
        return OperatingSystem.current().isWindows() ? "windows" : OperatingSystem.current().isMacOsX() ? "osx" : "linux";
    }
}