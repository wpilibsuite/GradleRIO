package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;

import edu.wpi.first.gradlerio.SingletonTask;

public class CppToolRunTask extends DefaultTask implements SingletonTask {
    private final String toolName;

    @Internal
    public String getToolName() {
        return toolName;
    }

    @Inject
    public CppToolRunTask(String name) {
        setGroup("GradleRIO");
        setDescription("Run the tool " + name);

        this.toolName = name;
    }

    @TaskAction
    public void runTool() {
        boolean isWindows = OperatingSystem.current().isWindows();
        if (isWindows) {
            runToolWindows();
        } else {
            runToolUnix();
        }
    }

    private String getArgumentPath(String toolNameLower) {
        return new File(getProject().getProjectDir(), "." + toolNameLower).getAbsolutePath();
    }

    private void runToolWindows() {
        File outputFile = new File(ToolInstallTask.getToolsFolder(), toolName + ".vbs");
        ProcessBuilder builder = new ProcessBuilder("wscript.exe", outputFile.getAbsolutePath(), "silent", getArgumentPath(toolName.toLowerCase()));
        try {
            Process proc = builder.start();
            int result = proc.waitFor();
            if (result != 0) {
                String stdOut = IOGroovyMethods.getText(proc.getInputStream());
                String stdErr = IOGroovyMethods.getText(proc.getErrorStream());
                throw new ToolRunException(stdOut, stdErr);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void runToolUnix() {
        File outputFile = new File(ToolInstallTask.getToolsFolder(), toolName + ".py");
        getProject().exec(spec -> {
            spec.setExecutable(outputFile.getAbsolutePath());
            spec.args(getArgumentPath(toolName.toLowerCase()));
        });
    }

    @Override
    @Internal
    public String getSingletonName() {
        return toolName;
    }
}
