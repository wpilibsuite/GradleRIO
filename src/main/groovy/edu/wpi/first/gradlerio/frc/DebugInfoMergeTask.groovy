package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.JsonMergeTask
import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;


@CompileStatic
class DebugInfoMergeTask extends JsonMergeTask {

    public static final String CONTAINER_FOLDER = "debug/riopartial"
    public static final String OUTPUT_FILE = "debug/debuginfo.json"

    @Inject
    public DebugInfoMergeTask(ObjectFactory objects) {
        super(objects);
        this.out.set(new File(project.rootProject.buildDir, OUTPUT_FILE))
        this.folder.set(new File(project.rootProject.buildDir, CONTAINER_FOLDER))
        this.singletonName = "mergeDebugInfo"
    }
}
