package edu.wpi.first.gradlerio.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLogging;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.jvm.tasks.Jar;

public class JavaTestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // project.getTasks().register("extractTestJNI", ExtractTestJNITask.class, t -> {
        //     t.setGroup("GradleRIO");
        //     t.setDescription("Extract Test JNI Native Libraries (nativeDesktopLib, nativeDesktopZip)");
        // });

        project.getTasks().register("simulateExternalJava", JavaExternalSimulationTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Simulate External Task for Java/Kotlin/JVM. Exports a JSON file for use by editors / tools");

            //task.dependsOn("extractTestJNI");
            task.dependsOn(project.getTasks().withType(Jar.class));
            task.finalizedBy(project.getTasks().withType(ExternalSimulationMergeTask.class));
        });

        project.getTasks().register("simulateJava", JavaSimulationTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Launch simulation for Java/Kotlin/JVM");

            //task.dependsOn("extractTestJNI");
            task.dependsOn(project.getTasks().withType(Jar.class));
        });
    }

    public static File jniExtractionDir(Project project) {
        return new File(project.getBuildDir(), "tmp/jniExtractDir");
    }
}
