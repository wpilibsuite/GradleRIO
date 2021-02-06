package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.JsonMergeTask
import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

@CompileStatic
class ExternalSimulationMergeTask extends JsonMergeTask {

    public static final String CONTAINER_FOLDER = "debug/partial"
    public static final String OUTPUT_FILE = "debug/desktopinfo.json"

    @Inject
    public ExternalSimulationMergeTask(ObjectFactory objects) {
        super(objects);
        this.out.set(new File(project.rootProject.buildDir, OUTPUT_FILE))
        this.folder.set(new File(project.rootProject.buildDir, CONTAINER_FOLDER))
        this.singletonName = "mergeExternalSim"
    }

}
