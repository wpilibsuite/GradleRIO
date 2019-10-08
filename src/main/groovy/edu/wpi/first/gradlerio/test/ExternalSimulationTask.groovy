package edu.wpi.first.gradlerio.test

import org.gradle.api.DefaultTask

class ExternalSimulationTask extends DefaultTask {

    ExternalSimulationTask() {
        outputs.upToDateWhen { false }
    }
 }