package org.wpilib.gradlerio.deploy;

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

public abstract class DebugFileTask extends DefaultTask {
    private final List<WPIRemoteTarget> targets = new ArrayList<>();

    @Internal
    public List<WPIRemoteTarget> getTargets() {
        return targets;
    }

    @OutputFile
    public abstract RegularFileProperty getDebugFile();

    @Inject
    public DebugFileTask(ObjectFactory objects) {
        getOutputs().upToDateWhen(spec -> false);
    }

    @TaskAction
    public void execute() throws IOException {
        List<DebugInfo> debugInfo = new ArrayList<>();
        for (WPIRemoteTarget wpiRemoteTarget : targets) {
            File debugFile = wpiRemoteTarget.debugFileTask.get().getDebugFile().get().getAsFile();
            debugInfo.add(new DebugInfo(wpiRemoteTarget.getName(), debugFile.getAbsolutePath()));
        }
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        File outputFile = getDebugFile().get().getAsFile();
        outputFile.getParentFile().mkdirs();
        ResourceGroovyMethods.setText(outputFile, builder.create().toJson(debugInfo));
    }
}
