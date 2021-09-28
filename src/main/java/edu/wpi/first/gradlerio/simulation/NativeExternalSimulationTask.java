package edu.wpi.first.gradlerio.simulation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeDependencySet;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.tasks.InstallExecutable;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;
import org.gradle.nativeplatform.toolchain.Clang;

import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class NativeExternalSimulationTask extends DefaultTask {

    private final List<NativeBinarySpec> binaries = new ArrayList<>();

    @Internal
    public List<NativeBinarySpec> getBinaries() {
        return binaries;
    }

    private InstallExecutable getInstallForBinary(NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            return (InstallExecutable)((NativeExecutableBinarySpec)binary).getTasks().getInstall();
        } else if (binary instanceof NativeTestSuiteBinarySpec) {
            return (InstallExecutable)((NativeTestSuiteBinarySpec)binary).getTasks().getInstall();
        } else {
            throw new GradleException("Unknown binary type");
        }
    }

    @Inject
    public NativeExternalSimulationTask(ObjectFactory objects) {
        getOutputs().upToDateWhen(spec -> false);
        Callable<Object[]> cbl = () -> binaries.stream().map(this::getInstallForBinary).toArray();
        dependsOn(cbl);
        simulationFile = objects.fileProperty();
    }

    private final RegularFileProperty simulationFile;

    @OutputFile
    public RegularFileProperty getSimulationFile() {
        return simulationFile;
    }

    public static class SimInfo {
        public final String type = "native";
        public String name;
        public List<HalSimPair> extensions;
        public String launchfile;
        public boolean clang;
        public Map<String, String> environment;

        public Object[] srcpaths;
        public Object[] headerpaths;
        public Object[] libpaths;
        public Object[] libsrcpaths;
        public SimInfo(String name, List<HalSimPair> extensions, String launchfile, boolean clang,
                Map<String, String> environment, List<File> srcpaths, List<File> headerpaths, List<File> libpaths, List<File> libsrcpaths) {
            this.name = name;
            this.extensions = extensions;
            this.launchfile = launchfile;
            this.clang = clang;
            this.environment = environment;
            this.srcpaths = srcpaths.stream().map(x -> x.getAbsolutePath()).toArray();
            this.headerpaths = headerpaths.stream().map(x -> x.getAbsolutePath()).toArray();
            this.libpaths = libpaths.stream().map(x -> x.getAbsolutePath()).toArray();
            this.libsrcpaths = libsrcpaths.stream().map(x -> x.getAbsolutePath()).toArray();
        }
    }

    @TaskAction
    public void execute() throws IOException {
        SimulationExtension sim = getProject().getExtensions().getByType(WPIExtension.class).getSim();

        List<SimInfo> simInfo = new ArrayList<>();


        Map<String, String> env = sim.getEnvironment();

        for (NativeBinarySpec binary : binaries) {
            InstallExecutable install = getInstallForBinary(binary);

            String name =  binary.getComponent().getName() + " (in project " + getProject().getName() + ")";

            String launchfile = install.getInstalledExecutable().get().getAsFile().getAbsolutePath();
            boolean clang = binary.getToolChain() instanceof Clang;

            List<File> srcpaths = new ArrayList<>();
            List<File> headerpaths = new ArrayList<>();
            List<File> libsrcpaths = new ArrayList<>();

            for (HeaderExportingSourceSet sourceSet : binary.getInputs().withType(HeaderExportingSourceSet.class)) {
                srcpaths.addAll(sourceSet.getSource().getSrcDirs());
                srcpaths.addAll(sourceSet.getExportedHeaders().getSrcDirs());
            }

            // Get all folders in install dir
            List<File> libpaths = new ArrayList<>(Arrays.asList(install.getInstallDirectory().get().getAsFile().listFiles(f -> f.isDirectory())));
            libpaths.add(install.getInstallDirectory().get().getAsFile());

            Map<Class<? extends NativeDependencySet>, Method> depClasses = new HashMap<>();

            for (NativeDependencySet ds : binary.getLibs()) {
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

            List<HalSimPair> extensions = sim.getHalSimLocations(libpaths, binary.getBuildType().getName().contains("debug"));

            simInfo.add(new SimInfo(name, extensions, launchfile, clang, env, srcpaths, headerpaths, libpaths, libsrcpaths));
        }

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = simulationFile.get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(simInfo));
    }
}
