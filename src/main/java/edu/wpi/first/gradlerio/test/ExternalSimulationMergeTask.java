package edu.wpi.first.gradlerio.test;

import edu.wpi.first.gradlerio.JsonMergeTask;
import org.gradle.api.model.ObjectFactory;

import java.io.File;

import javax.inject.Inject;

public class ExternalSimulationMergeTask extends JsonMergeTask {

    public static final String CONTAINER_FOLDER = "debug/partial";
    public static final String OUTPUT_FILE = "debug/desktopinfo.json";

    @Inject
    public ExternalSimulationMergeTask(ObjectFactory objects) {
        super(objects);
        this.getOut().set(new File(getProject().getRootProject().getBuildDir(), OUTPUT_FILE));
        this.getFolder().set(new File(getProject().getRootProject().getBuildDir(), CONTAINER_FOLDER));
        this.setSingletonName("mergeExternalSim");
    }

}
