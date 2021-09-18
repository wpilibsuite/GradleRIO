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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.ExecSpec;

public class ToolInstallTask extends DefaultTask {
    private final String toolName;
    private final Configuration configuration;
    private final String artifactName;
    private static final Type mapType = new TypeToken<List<ToolConfig>>() {
    }.getType();

    private static File toolsFolder;

    public static void setToolsFolder(File toolsFolder) {
        ToolInstallTask.toolsFolder = toolsFolder;
    }

    public static File getToolsFolder() {
        return toolsFolder;
    }

    @Internal
    public String getToolName() {
        return toolName;
    }

    @Internal
    public Configuration getConfiguration() {
        return configuration;
    }

    @Internal
    public String getArtifactName() {
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
    public ToolInstallTask(String toolName, Configuration configuration, String artifactName) {
        setGroup("GradleRIO");
        setDescription("Install the tool " + toolName);

        this.toolName = toolName;
        this.configuration = configuration;
        this.artifactName = artifactName;
    }

    private static synchronized Optional<ToolConfig> getExistingToolVersion(String toolName) {
        // Load JSON file
        File toolFile = new File(toolsFolder, "tools.json");
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

    private static synchronized void setToolVersion(ToolConfig tool) {
        File toolFile = new File(toolsFolder, "tools.json");
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

    private File getScriptFile() {
        return new File(toolsFolder, toolName + ".vbs");
    }

    private Dependency getDependencyObject() {
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
        boolean jarExists = new File(toolsFolder, toolName + ".jar").exists();
        boolean scriptExists = getScriptFile().exists();

        Dependency dependency = getDependencyObject();

        if (dependency == null) {
            throw new GradleException("Tool " + artifactName + " not found in dependency list");
        }

        if (!jarExists || !scriptExists) {
            extractAndInstall(dependency);
            return;
        }

        Optional<ToolConfig> existingVersion = getExistingToolVersion(toolName);
        if (existingVersion.isEmpty()) {
            extractAndInstall(dependency);
            return;
        }

        // Check version
        if (dependency.getVersion().compareTo(existingVersion.get().version) > 0) {
            extractAndInstall(dependency);
        }
    }

    private void extractAndInstall(Dependency dependency) {
        File jarfile = configuration.files(dependency).iterator().next();
        File of = toolsFolder;
        of.mkdirs();
        getProject().copy(new Action<CopySpec>() {
            @Override
            public void execute(CopySpec cp) {
                cp.from(jarfile);
                cp.into(of);
                cp.rename(f -> toolName + ".jar");
            }
        });
        if (OperatingSystem.current().isWindows()) {
            extractScriptWindows();
        } else {
            extractScriptUnix();
        }
        ToolConfig ToolConfig = new ToolConfig();
        ToolConfig.name = toolName;
        ToolConfig.version = dependency.getVersion();
        ToolConfig.cpp = false;
        setToolVersion(ToolConfig);
    }

    private void extractScriptWindows() {

        File outputFile = new File(toolsFolder, toolName + ".vbs");
        try (InputStream it = ToolInstallTask.class.getResourceAsStream("/ScriptBase.vbs")) {
            ResourceGroovyMethods.setText(outputFile, IOGroovyMethods.getText(it));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractScriptUnix() {
        File outputFile = new File(toolsFolder, toolName + ".py");
        try (InputStream it = ToolInstallTask.class.getResourceAsStream("/ScriptBase.py")) {
            ResourceGroovyMethods.setText(outputFile, IOGroovyMethods.getText(it));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getProject().exec(new Action<ExecSpec>() {

            @Override
            public void execute(ExecSpec spec) {
                spec.setCommandLine("chmod");
                spec.args("0775", outputFile.getAbsolutePath());
            }

        });
    }
}
