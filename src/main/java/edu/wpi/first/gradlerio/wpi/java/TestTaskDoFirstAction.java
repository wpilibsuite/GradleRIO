package edu.wpi.first.gradlerio.wpi.java;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.JavaForkOptions;

public class TestTaskDoFirstAction implements Action<Task> {

    private final JavaForkOptions t;
    private final Provider<DirectoryProperty> extract;

    public TestTaskDoFirstAction(JavaForkOptions t, Provider<DirectoryProperty> extract) {
        this.t = t;
        this.extract = extract;
    }

    @Override
    public void execute(Task arg0) {
        Map<String, String> env = new HashMap<>();

        String ldpath = extract.get().get().getAsFile().getAbsolutePath();

        if (OperatingSystem.current().isUnix()) {
            appendPath(env, "LD_LIBRARY_PATH", ldpath);
            appendPath(env, "DYLD_FALLBACK_LIBRARY_PATH", ldpath);
            appendPath(env, "DYLD_LIBRARY_PATH", ldpath);
        } else if (OperatingSystem.current().isWindows()) {
            appendPath(env, "PATH", ldpath);
        }

        t.environment(env);

        String jlp = ldpath;

        if (t.getSystemProperties().containsKey("java.library.path")) {
            jlp = (String) t.getSystemProperties().get("java.library.path") + File.pathSeparator + ldpath;
        }
        t.getSystemProperties().put("java.library.path", jlp);

    }

    private static void appendPath(Map<String, String> env, String envName, String ldpath) {
        String currentPath = System.getenv(envName);
        if (currentPath != null) {
            env.put(envName, currentPath + File.pathSeparator + ldpath);
        } else {
            env.put(envName, ldpath);
        }
    }
}
