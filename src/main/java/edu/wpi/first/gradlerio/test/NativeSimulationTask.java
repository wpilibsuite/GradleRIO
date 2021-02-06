package edu.wpi.first.gradlerio.test;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.tasks.InstallExecutable;

import edu.wpi.first.gradlerio.ExternalLaunchTask;

public class NativeSimulationTask extends ExternalLaunchTask {

    @Inject
    public NativeSimulationTask(ObjectFactory objects) {
        super(objects);
    }

    private NativeExecutableBinarySpec binary;

    @Internal
    public NativeExecutableBinarySpec getBinary() {
        return binary;
    }

    public void setBinary(NativeExecutableBinarySpec binary) {
        this.binary = binary;
    }

    @TaskAction
    public void run() {
        InstallExecutable installTask = binary.getTasks().withType(InstallExecutable.class).iterator().next();
        File dir = new File(installTask.getInstallDirectory().getAsFile().get(), "lib");

        getEnvironment().putAll(TestPlugin.getSimLaunchEnv(getProject(), dir.getAbsolutePath()));

        getWorkingDir().set(dir);

        launch("\"" + installTask.getRunScriptFile().get().getAsFile().getAbsolutePath() + "\"");
    }

}
