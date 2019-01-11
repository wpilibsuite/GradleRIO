package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.JsonMergeTask
import groovy.transform.CompileStatic

@CompileStatic
class ExternalSimulationMergeTask extends JsonMergeTask {

    public static final String CONTAINER_FOLDER = "debug/partial"
    public static final String OUTPUT_FILE = "debug/desktopinfo.json"

    ExternalSimulationMergeTask() {
        this.out = new File(project.rootProject.buildDir, OUTPUT_FILE)
        this.folder = new File(project.rootProject.buildDir, CONTAINER_FOLDER)
        this.singletonName = "mergeExternalSim"
    }

}
