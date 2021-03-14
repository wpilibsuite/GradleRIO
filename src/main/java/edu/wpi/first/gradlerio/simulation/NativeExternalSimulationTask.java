package edu.wpi.first.gradlerio.simulation;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;

public class NativeExternalSimulationTask extends DefaultTask {

    @Inject
    public NativeExternalSimulationTask() {
        getOutputs().upToDateWhen(spec -> false);
    }
}
