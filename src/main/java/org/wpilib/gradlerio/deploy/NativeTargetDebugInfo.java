package org.wpilib.gradlerio.deploy;

import java.io.File;
import java.util.List;

public class NativeTargetDebugInfo extends TargetDebugInfo {
    public final String type = "native";
    public String name;
    public int port;
    public String target;
    public String launchfile;
    public String gdb;
    public String sysroot;
    public Object[] srcpaths;
    public Object[] headerpaths;
    public Object[] libpaths;
    public Object[] libsrcpaths;

    public NativeTargetDebugInfo(String name, int port, String target, String launchfile, String gdb, String sysroot,
            List<File> srcpaths, List<File> headerpaths, List<File> libpaths, List<File> libsrcpaths) {
        this.name = name;
        this.port = port;
        this.target = target;
        this.launchfile = launchfile;
        this.gdb = gdb;
        this.sysroot = sysroot;
        this.srcpaths = srcpaths.stream().map(x -> x.getAbsolutePath()).toArray();
        this.headerpaths = headerpaths.stream().map(x -> x.getAbsolutePath()).toArray();
        this.libpaths = libpaths.stream().map(x -> x.getAbsolutePath()).toArray();
        this.libsrcpaths = libsrcpaths.stream().map(x -> x.getAbsolutePath()).toArray();
    }
}
