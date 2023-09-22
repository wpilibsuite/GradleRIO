package edu.wpi.first.gradlerio.wpi.dependencies.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.ExecSpec;

public class ToolInstallTask extends DefaultTask {

    private static final Type mapType = new TypeToken<List<ToolConfig>>() {
    }.getType();

    private final DirectoryProperty toolsFolder;
    private final Property<Configuration> configuration;
    private final Property<String> toolName;
    private final Property<String> artifactName;

    @Internal
    public DirectoryProperty getToolsFolder() {
        return toolsFolder;
    }

    @Internal
    public Property<String> getToolName() {
        return toolName;
    }

    @Internal
    public Property<Configuration> getConfiguration() {
        return configuration;
    }

    @Internal
    public Property<String> getArtifactName() {
        return artifactName;
    }

    @SuppressWarnings("unused")
    private static class ArtifactConfig {
        public String classifier;
        public String extension;
        public String groupId;
        public String artifactId;
        public String version;
    }

    @SuppressWarnings("unused")
    private static class ToolConfig {
        public String name;
        public String version;
        public ArtifactConfig artifactConfig;
        public boolean cpp;
    }

    @Inject
    public ToolInstallTask(ObjectFactory objects) {
        setGroup("GradleRIO");

        toolsFolder = objects.directoryProperty();
        configuration = objects.property(Configuration.class);
        toolName = objects.property(String.class);
        artifactName = objects.property(String.class);
    }

    private static synchronized Optional<ToolConfig> getExistingToolVersion(Directory toolsFolder, String toolName) {
        // Load JSON file
        File toolFile = toolsFolder.file("tools.json").getAsFile();
        if (toolFile.exists()) {
            String toolTxt;
            try {
                toolTxt = ResourceGroovyMethods.getText(toolFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Gson gson = new Gson();
            ToolConfig[] tools = gson.fromJson(toolTxt, ToolConfig[].class);
            return Stream.of(tools).filter(it -> it.name.equals(toolName)).findFirst();
        } else {
            return Optional.empty();
        }
    }

    private static synchronized void setToolVersion(Directory toolsFolder, ToolConfig tool) {
        File toolFile = toolsFolder.file("tools.json").getAsFile();
        Gson gson = new Gson();
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        try {
            if (toolFile.exists()) {
                String toolTxt;

                toolTxt = ResourceGroovyMethods.getText(toolFile);

                List<ToolConfig> tools = gson.fromJson(toolTxt, mapType);
                tools.removeIf(x -> x.name.equals(tool.name));
                tools.add(tool);
                String json = builder.create().toJson(tools);
                ResourceGroovyMethods.setText(toolFile, json);
            } else {
                ToolConfig[] tools = new ToolConfig[] { tool };
                String json = builder.create().toJson(tools);
                ResourceGroovyMethods.setText(toolFile, json);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getScriptFile(Directory toolsFolder, String toolName) {
        return toolsFolder.file(toolName + ".vbs").getAsFile();
    }

    private static Dependency getDependencyObject(Configuration configuration, String artifactName) {
        for (Dependency dep : configuration.getDependencies()) {
            if (dep.getName().equals(artifactName)) {
                return dep;
            }
        }
        return null;
    }

    @TaskAction
    public void installTool() {
        // First check to see if both script and jar exist
        Directory toolsFolder = this.toolsFolder.get();
        String toolName = this.toolName.get();
        boolean jarExists = toolsFolder.file(toolName + ".jar").getAsFile().exists();
        boolean scriptExists = getScriptFile(toolsFolder, toolName).exists();
        Configuration configuration = this.configuration.get();

        Dependency dependency = getDependencyObject(configuration, this.artifactName.get());

        if (dependency == null) {
            throw new GradleException("Tool " + artifactName + " not found in dependency list");
        }

        if (!jarExists || !scriptExists) {
            extractAndInstall(getProject(), toolName, toolsFolder, dependency, configuration);
            return;
        }

        Optional<ToolConfig> existingVersion = getExistingToolVersion(toolsFolder, toolName);
        if (existingVersion.isEmpty()) {
            extractAndInstall(getProject(), toolName, toolsFolder, dependency, configuration);
            return;
        }

        // Check version
        if (dependency.getVersion().compareTo(existingVersion.get().version) > 0) {
            extractAndInstall(getProject(), toolName, toolsFolder, dependency, configuration);
        }
    }

    private static void extractAndInstall(Project project, String toolName, Directory toolsFolder,
            Dependency dependency, Configuration configuration) {
        File jarfile = configuration.files(dependency).iterator().next();
        File of = toolsFolder.getAsFile();
        of.mkdirs();
        project.copy(new Action<CopySpec>() {
            @Override
            public void execute(CopySpec cp) {
                cp.from(jarfile);
                cp.into(of);
                cp.rename(f -> toolName + ".jar");
            }
        });
        if (OperatingSystem.current().isWindows()) {
            extractScriptWindows(toolsFolder, toolName);
        } else {
            extractScriptUnix(project, toolsFolder, toolName);
        }
        ToolConfig ToolConfig = new ToolConfig();
        ToolConfig.name = toolName;
        ToolConfig.version = dependency.getVersion();
        ToolConfig.cpp = false;
        setToolVersion(toolsFolder, ToolConfig);
    }

    private static void extractScriptWindows(Directory toolsFolder, String toolName) {
        File outputFile = toolsFolder.file(toolName + ".vbs").getAsFile();
        try (InputStream it = ToolInstallTask.class.getResourceAsStream("/ScriptBase.vbs")) {
            ResourceGroovyMethods.setText(outputFile, IOGroovyMethods.getText(it));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractScriptUnix(Project project, Directory toolsFolder, String toolName) {
        File outputFile = toolsFolder.file(toolName + ".py").getAsFile();
        try (InputStream it = ToolInstallTask.class.getResourceAsStream("/ScriptBase.py")) {
            ResourceGroovyMethods.setText(outputFile, IOGroovyMethods.getText(it));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        project.exec(new Action<ExecSpec>() {

            @Override
            public void execute(ExecSpec spec) {
                spec.setCommandLine("chmod");
                spec.args("0775", outputFile.getAbsolutePath());
            }

        });
    }
}
