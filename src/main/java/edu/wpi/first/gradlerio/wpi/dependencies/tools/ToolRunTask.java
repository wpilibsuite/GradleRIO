package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.os.OperatingSystem;

import edu.wpi.first.gradlerio.SingletonTask;
import groovy.transform.CompileStatic;

@CompileStatic
public class ToolRunTask extends DefaultTask implements SingletonTask {

    private final TaskProvider<ToolInstallTask> installTask;
    private final String toolName;

    @Internal
    public String getToolName() {
        return toolName;
    }

    @Internal
    public TaskProvider<ToolInstallTask> getInstallTask() {
        return installTask;
    }

    @Inject
    public ToolRunTask(String name, TaskProvider<ToolInstallTask> installTask) {
        setGroup("GradleRIO");
        setDescription("Run the tool " + name);


        this.toolName = name;
        this.installTask = installTask;
        dependsOn(installTask);
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

    private void runToolWindows() {
        ToolInstallTask iTask = installTask.get();
        File outputFile = new File(ToolInstallTask.getToolsFolder(), iTask.getToolName() + ".vbs");
        ProcessBuilder builder = new ProcessBuilder("wscript.exe", outputFile.getAbsolutePath(), "silent");
        Process proc;
        try {
            proc = builder.start();
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
        ToolInstallTask iTask = installTask.get();
        File outputFile = new File(ToolInstallTask.getToolsFolder(), iTask.getToolName() + ".py");
        getProject().exec(spec -> {
            spec.setExecutable(outputFile.getAbsolutePath());
        });
    }

    @Override
    @Internal
    public String getSingletonName() {
        return toolName;
    }
}
