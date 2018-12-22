package edu.wpi.first.toolchain.raspbian;

import edu.wpi.first.toolchain.*;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class RaspbianToolchainPlugin implements Plugin<Project> {

    public static final String toolchainName = "raspbian";

    private RaspbianToolchainExtension raspbianExt;
    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;

        raspbianExt = project.getExtensions().create("raspbianToolchain", RaspbianToolchainExtension.class);

        ToolchainExtension toolchainExt = project.getExtensions().getByType(ToolchainExtension.class);

        ToolchainDescriptor descriptor = new ToolchainDescriptor(toolchainName, "raspianGcc", new ToolchainRegistrar<RaspbianGcc>(RaspbianGcc.class, project));
        descriptor.setToolchainPlatforms(NativePlatforms.raspbian);
        descriptor.setOptional(true);
        descriptor.getDiscoverers().all((ToolchainDiscoverer disc) -> {
            disc.configureVersions(raspbianExt.versionLow, raspbianExt.versionHigh);
        });

        toolchainExt.add(descriptor);

        project.afterEvaluate((Project proj) -> {
            populateDescriptor(descriptor);
        });
    }

    public static File toolchainInstallLoc(String vers) {
        return new File(ToolchainPlugin.pluginHome(), vers);
    }

    public String composeTool(String toolName) {
        String raspbianVersion = raspbianExt.toolchainVersion.split("-")[0].toLowerCase();
        String exeSuffix = OperatingSystem.current().isWindows() ? ".exe" : "";
        return "arm-" + raspbianVersion + "-linux-gnueabihf-" + toolName + exeSuffix;
    }

    public void populateDescriptor(ToolchainDescriptor descriptor) {
        String raspbianVersion = raspbianExt.toolchainVersion.split("-")[0].toLowerCase();
        File installLoc = toolchainInstallLoc(raspbianVersion);

        descriptor.getDiscoverers().add(new ToolchainDiscoverer("GradleUserDir", installLoc, this::composeTool));
        descriptor.getDiscoverers().addAll(ToolchainDiscoverer.forSystemPath(project, this::composeTool));

        try {
            descriptor.getInstallers().add(installerFor(OperatingSystem.LINUX, installLoc, raspbianVersion));
            descriptor.getInstallers().add(installerFor(OperatingSystem.WINDOWS, installLoc, raspbianVersion));
            descriptor.getInstallers().add(installerFor(OperatingSystem.MAC_OS, installLoc, raspbianVersion));
        } catch (MalformedURLException e) {
            throw new GradleException("Malformed Toolchain URL", e);
        }
    }

    private AbstractToolchainInstaller installerFor(OperatingSystem os, File installDir, String subdir) throws MalformedURLException {
        URL url = toolchainDownloadUrl(toolchainRemoteFile());
        return new DefaultToolchainInstaller(os, url, installDir, subdir);
    }

    private String toolchainRemoteFile() {
        String[] desiredVersion = raspbianExt.toolchainVersion.split("-");

        String platformId = OperatingSystem.current().isWindows() ? "Windows" : OperatingSystem.current().isMacOsX() ? "Mac" : "Linux";
        String ext = OperatingSystem.current().isWindows() ? "zip" : "tar.gz";
        return desiredVersion[0] + "-" + platformId + "-Toolchain-" + desiredVersion[1] + "." + ext;
    }

    private URL toolchainDownloadUrl(String file) throws MalformedURLException {
        return new URL("https://github.com/wpilibsuite/raspbian-toolchain/releases/download/" + raspbianExt.toolchainTag + "/" + file);
    }

}
