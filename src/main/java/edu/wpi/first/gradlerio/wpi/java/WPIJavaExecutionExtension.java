package edu.wpi.first.gradlerio.wpi.java;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLogging;
import org.gradle.internal.os.OperatingSystem;

public class WPIJavaExecutionExtension {
    @Inject
    public WPIJavaExecutionExtension(Project project, WPIJavaExtension javaExt) {
        project.getTasks().withType(Test.class).configureEach(t -> {

            Provider<ExtractNativeJavaArtifacts> extract = project.getProviders().provider(() -> {
                if (javaExt.getDebugJni().get()) {
                    return javaExt.getExtractNativeDebugArtifacts().get();
                } else {
                    return javaExt.getExtractNativeReleaseArtifacts().get();
                }
            });

            t.dependsOn(extract);

            t.doFirst(new Action<Task>() {
				@Override
				public void execute(Task arg0) {
                    Map<String, String> env = new HashMap<>();

                    String ldpath = extract.get().getDestinationDirectory().get().getAsFile().getAbsolutePath();

                    if (OperatingSystem.current().isUnix()) {
                        env.put("LD_LIBRARY_PATH", ldpath);
                        env.put("DYLD_FALLBACK_LIBRARY_PATH", ldpath);
                        env.put("DYLD_LIBRARY_PATH", ldpath);
                    } else if (OperatingSystem.current().isWindows()) {
                        env.put("PATH", System.getenv("PATH") + File.pathSeparator + ldpath);
                    }

                    t.environment(env);

                    String jlp = ldpath;

                    if (t.getSystemProperties().containsKey("java.library.path")) {
                        jlp = (String)t.getSystemProperties().get("java.library.path") + File.pathSeparator + ldpath;
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
}
