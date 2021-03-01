package edu.wpi.first.gradlerio.wpi.simulation;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public abstract class SimulationExtractTask extends DefaultTask {
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract Property<Configuration> getSimConfiguration();

    @TaskAction
    public void execute() {
        Configuration config = getSimConfiguration().get();

        String[] matchers = new String[] {"**/*.so*", "**/*.so", "**/*.dll", "**/*.dylib"};
        var filter = new PatternSet();
        filter.include(matchers);

        config.getAsFileTree().matching(filter).getFiles();

    }
}
