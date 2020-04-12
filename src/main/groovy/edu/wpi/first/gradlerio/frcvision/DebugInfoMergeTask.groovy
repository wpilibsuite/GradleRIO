package edu.wpi.first.gradlerio.frcvision

import edu.wpi.first.gradlerio.JsonMergeTask
import groovy.transform.CompileStatic

@CompileStatic
class DebugInfoMergeTask extends JsonMergeTask {

    public static final String CONTAINER_FOLDER = "debug/raspbianpartial"
    public static final String OUTPUT_FILE = "debug/debuginfo.json"

    DebugInfoMergeTask() {
        this.out = new File(project.rootProject.buildDir, OUTPUT_FILE)
        this.folder = new File(project.rootProject.buildDir, CONTAINER_FOLDER)
        this.singletonName = "mergeDebugInfo"
    }
}
