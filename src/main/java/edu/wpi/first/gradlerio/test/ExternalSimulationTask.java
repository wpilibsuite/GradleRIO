package edu.wpi.first.gradlerio.test;

import org.gradle.api.DefaultTask;

public class ExternalSimulationTask extends DefaultTask {
    public ExternalSimulationTask() {
        getOutputs().upToDateWhen((x) -> false);
    }
 }
