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
        ProcessBuilder builder = getProcessBuilder();
        try {
            Process proc = builder.start();
            // Wait 3 seconds
            Thread.sleep(3000);
            if (!proc.isAlive()) {
                String stdOut = IOGroovyMethods.getText(proc.getInputStream());
                String stdErr = IOGroovyMethods.getText(proc.getErrorStream());
                throw new ToolRunException(stdOut, stdErr);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getArgumentPath(String toolNameLower) {
        return new File(getProject().getProjectDir(), "." + toolNameLower).getAbsolutePath();
    }

    private ProcessBuilder getProcessBuilder() {
        String toolNameLower = toolName.toLowerCase();
        if (OperatingSystem.current().isWindows()) {
            String outputFile = new File(ToolInstallTask.getToolsFolder(), toolName + ".exe").getAbsolutePath();
            return new ProcessBuilder(outputFile, getArgumentPath(toolNameLower));
        } else if (OperatingSystem.current().isMacOsX()) {
            String outputFile = new File(ToolInstallTask.getToolsFolder(), toolName + ".app").getAbsolutePath();
            return new ProcessBuilder("open", outputFile, getArgumentPath(toolNameLower));
        } else {
            String outputFile = new File(ToolInstallTask.getToolsFolder(), toolNameLower).getAbsolutePath();
            return new ProcessBuilder(outputFile, getArgumentPath(toolNameLower));
        }
    }

    @Override
    @Internal
    public String getSingletonName() {
        return toolName;
    }
}
