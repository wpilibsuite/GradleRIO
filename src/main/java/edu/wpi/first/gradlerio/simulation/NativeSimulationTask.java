package edu.wpi.first.gradlerio.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.AbstractExecTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.tasks.InstallExecutable;

import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class NativeSimulationTask extends AbstractExecTask<NativeSimulationTask> {
    private final List<NativeBinarySpec> binaries = new ArrayList<>();

    @Internal
    public List<NativeBinarySpec> getBinaries() {
        return binaries;
    }

    private InstallExecutable getInstallForBinary(NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            return (InstallExecutable)((NativeExecutableBinarySpec)binary).getTasks().getInstall();
        } else {
            throw new GradleException("Unknown binary type");
        }
    }

    private final Callable<Object[]> cbl;

    @Inject
    public NativeSimulationTask() {
        super(NativeSimulationTask.class);
        getOutputs().upToDateWhen(spec -> false);
        cbl = () -> binaries.stream().map(this::getInstallForBinary).toArray();
        dependsOn(cbl);
    }

    @TaskAction
    @Override
    protected void exec() {
        for (NativeBinarySpec binary : binaries) {
            if (!(binary instanceof NativeExecutableBinarySpec)) {
                binaries.remove(binary);
            }
        }

        if (binaries.size() != 1) {
             throw new GradleException("Must have 1 and only 1 binary");
        }

        NativeExecutableBinarySpec binary = (NativeExecutableBinarySpec)binaries.get(0);
        InstallExecutable install = (InstallExecutable)binary.getTasks().getInstall();

        setExecutable(install.getRunScriptFile().get().getAsFile());

        List<File> libpaths = new ArrayList<>(Arrays.asList(install.getInstallDirectory().get().getAsFile().listFiles(f -> f.isDirectory())));
        libpaths.add(install.getInstallDirectory().get().getAsFile());

        SimulationExtension sim = getProject().getExtensions().getByType(WPIExtension.class).getSim();

        List<HalSimPair> extensions = sim.getHalSimLocations(libpaths, binary.getBuildType().getName().contains("debug"));
        Optional<String> extensionString = extensions.stream().filter(x -> x.defaultEnabled).map(x -> x.libName).reduce((a, b) -> a + File.pathSeparator + b);
        if (extensionString.isPresent()) {
            environment("HALSIM_EXTENSIONS", extensionString.get());
        }

        super.exec();
    }

}
