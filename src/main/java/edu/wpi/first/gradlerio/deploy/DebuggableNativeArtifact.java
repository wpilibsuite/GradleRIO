package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.nativeplatform.NativeDependencySet;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;

import edu.wpi.first.deployutils.deploy.artifact.NativeExecutableArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.sessions.IPSessionController;
import edu.wpi.first.toolchain.ToolchainDiscoverer;
import edu.wpi.first.toolchain.ToolchainExtension;
import edu.wpi.first.toolchain.roborio.RoboRioToolchainPlugin;

public class DebuggableNativeArtifact extends NativeExecutableArtifact implements DebuggableArtifact {

    private int debugPort = 8349;

    public int getDebugPort() {
        return debugPort;
    }

    @Inject
    public DebuggableNativeArtifact(String name, WPIRemoteTarget target) {
        super(name, target);
    }

    @Override
    public TargetDebugInfo getTargetDebugInfo() {
        DeployContext ctx = getTarget().getTargetDiscoveryTask().get().getActiveContext();

        if (ctx.getController() instanceof IPSessionController) {
            IPSessionController session = (IPSessionController) ctx.getController();

            // TODO get all this from the VS Code plugin without needing to enumerate EVERYTHING

            ToolchainDiscoverer toolchain = getTarget().getProject().getExtensions().getByType(ToolchainExtension.class)
                    .getToolchainDescriptors().getByName(RoboRioToolchainPlugin.toolchainName).discover();
            String gdbpath = toolchain.gdbFile().get().getAbsolutePath();
            String sysroot = toolchain.sysroot().get().getAbsolutePath();

            List<File> srcpaths = new ArrayList<>();
            List<File> headerpaths = new ArrayList<>();
            List<File> libsrcpaths = new ArrayList<>();

            NativeExecutableBinarySpec bin = getBinary().get();
            for (HeaderExportingSourceSet sourceSet : bin.getInputs().withType(HeaderExportingSourceSet.class)) {
                srcpaths.addAll(sourceSet.getSource().getSrcDirs());
                srcpaths.addAll(sourceSet.getExportedHeaders().getSrcDirs());
            }

            // Get all folders in install dir
            List<File> libpaths = new ArrayList<>(Arrays.asList(getInstallTaskProvider().get().getInstallDirectory().get().getAsFile().listFiles(f -> f.isDirectory())));
            libpaths.add(getInstallTaskProvider().get().getInstallDirectory().get().getAsFile());


            Map<Class<? extends NativeDependencySet>, Method> depClasses = new HashMap<>();

            for (NativeDependencySet ds : bin.getLibs()) {
                headerpaths.addAll(ds.getIncludeRoots().getFiles());

                Class<? extends NativeDependencySet> cls = ds.getClass();
                Method sourceMethod = null;
                if (depClasses.containsKey(cls)) {
                    sourceMethod = depClasses.get(cls);
                } else {
                    try {
                        sourceMethod = cls.getDeclaredMethod("getSourceFiles");
                    } catch (NoSuchMethodException | SecurityException e) {
                        sourceMethod = null;
                    }
                    depClasses.put(cls, sourceMethod);
                }
                if (sourceMethod != null) {
                    try {
                        Object rootsObject = sourceMethod.invoke(ds);
                        if (rootsObject instanceof FileCollection) {
                            libsrcpaths.addAll(((FileCollection) rootsObject).getFiles());
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

            }

            return new NativeTargetDebugInfo(debugPort, session.getHost(),
                getDeployedFile().getAbsolutePath(), gdbpath, sysroot, srcpaths, headerpaths, libpaths, libsrcpaths);
        }
        return null;
    }

}
