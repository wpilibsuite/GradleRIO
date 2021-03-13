package edu.wpi.first.gradlerio.deploy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import edu.wpi.first.deployutils.deploy.target.RemoteTarget;

public class TargetDebugFileTask extends DefaultTask {

    private RemoteTarget target;

    @Internal
    public RemoteTarget getTarget() {
        return target;
    }

    public void setTarget(RemoteTarget target) {
        this.target = target;
    }

    private final RegularFileProperty debugFile;

    @OutputFile
    public RegularFileProperty getDebugFile() {
        return debugFile;
    }

    @Inject
    public TargetDebugFileTask(ObjectFactory objects) {
        getOutputs().upToDateWhen((spec) -> false);
        debugFile = objects.fileProperty();
    }

    @TaskAction
    public void execute() throws IOException {
        List<TargetDebugInfo> debugList = new ArrayList<>();
        for (DebuggableArtifact artifact : target.getArtifacts().withType(DebuggableArtifact.class)) {
            debugList.add(artifact.getTargetDebugInfo());
        }
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = debugFile.get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(debugList));
    }
}
