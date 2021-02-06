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
        project.getTasks().register("extractTestJNI", ExtractTestJNITask.class, t -> {
            t.setGroup("GradleRIO");
            t.setDescription("Extract Test JNI Native Libraries (nativeDesktopLib, nativeDesktopZip)");
        });

        project.getTasks().register("simulateExternalJava", JavaExternalSimulationTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Simulate External Task for Java/Kotlin/JVM. Exports a JSON file for use by editors / tools");

            task.dependsOn("extractTestJNI");
            task.dependsOn(project.getTasks().withType(Jar.class));
            task.finalizedBy(project.getTasks().withType(ExternalSimulationMergeTask.class));
        });

        project.getTasks().register("simulateJava", JavaSimulationTask.class, task -> {
            task.setGroup("GradleRIO");
            task.setDescription("Launch simulation for Java/Kotlin/JVM");

            task.dependsOn("extractTestJNI");
            task.dependsOn(project.getTasks().withType(Jar.class));
        });

        project.getTasks().withType(Test.class).configureEach(t -> {
            t.dependsOn("extractTestJNI");

            t.doFirst(new Action<Task>() {
				@Override
				public void execute(Task arg0) {
                    Map<String, String> env = new HashMap<>();

                    String ldpath = jniExtractionDir(project).getAbsolutePath();

                    if (OperatingSystem.current().isUnix()) {
                        env.put("LD_LIBRARY_PATH", ldpath);
                        env.put("DYLD_FALLBACK_LIBRARY_PATH", ldpath);
                        env.put("DYLD_LIBRARY_PATH", ldpath);
                    } else if (OperatingSystem.current().isWindows()) {
                        env.put("PATH", System.getenv("PATH") + TestPlugin.envDelimiter() + ldpath);
                    }

                    t.environment(env);

                    String jlp = ldpath;

                    if (t.getSystemProperties().containsKey("java.library.path")) {
                        jlp = (String)t.getSystemProperties().get("java.library.path") + TestPlugin.envDelimiter() + ldpath;
                    }
                    t.getSystemProperties().put("java.library.path", jlp);
				}
            });

            t.testLogging(new Action<TestLogging>() {
				@Override
				public void execute(TestLogging log) {
                    log.events(TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_ERROR,
                        TestLogEvent.STANDARD_OUT);
                    log.setShowStandardStreams(true);
				}
            });
        });
    }

    public static File jniExtractionDir(Project project) {
        return new File(project.getBuildDir(), "tmp/jniExtractDir");
    }
}
