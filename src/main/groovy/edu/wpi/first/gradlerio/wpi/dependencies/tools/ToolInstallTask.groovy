package edu.wpi.first.gradlerio.wpi.dependencies.tools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

import javax.inject.Inject

@CompileStatic
class ToolInstallTask extends DefaultTask {
    @Internal
    String toolName
    @Internal
    Configuration configuration
    @Internal
    Dependency dependency
    static String toolsFolder

    private static class ToolJson {
        String name
        String version
        Boolean cpp
    }

    @Inject
    ToolInstallTask(String toolName, Configuration configuration, Dependency dep) {
        group = 'GradleRIO'
        description = "Install the tool $toolName"

        this.toolName = toolName
        this.configuration = configuration
        this.dependency = dep
    }

    static synchronized ToolJson getExistingToolVersion(String toolName) {
        // Load JSON file
        def toolFile = new File(toolsFolder, 'tools.json')
        if (toolFile.exists()) {
            def toolTxt = toolFile.text
            Gson gson = new Gson()
            ToolJson[] tools = gson.fromJson(toolTxt, ToolJson[].class)
            ToolJson tool = tools.find {
                it.name == toolName
            }
            return tool
        } else {
            return null
        }
    }

    @CompileDynamic
    static synchronized void setToolVersion(ToolJson tool) {
        def toolFile = new File(toolsFolder, 'tools.json')
        def gson = new Gson()
        def builder = new GsonBuilder()
        builder.setPrettyPrinting()
        if (toolFile.exists()) {
            def toolTxt = toolFile.text
            def tools = gson.fromJson(toolTxt, Object[].class).toList()
            tools.removeIf {
                it.name == tool.name
            }
            tools << tool
            def json = builder.create().toJson(tools)
            toolFile.text = json
        } else {
            ToolJson[] tools = [tool]
            def json = builder.create().toJson(tools)
            toolFile.text = json
        }
    }

    private File getScriptFile() {
        return new File(toolsFolder, toolName + '.vbs')
    }

    @TaskAction
    void installTool() {
        // First check to see if both script and jar exist
        def jarExists = new File(toolsFolder, toolName + '.jar').exists()
        def scriptExists = scriptFile.exists()

        if (!jarExists || !scriptExists) {
            extractAndInstall()
            return
        }

        def existingVersion = getExistingToolVersion(toolName)
        if (existingVersion == null) {
            extractAndInstall()
            return
        }

        if (existingVersion != null) {
            // Check version
            if (dependency.version > existingVersion.version) {
                extractAndInstall()
            }
        }


    }

    private void extractAndInstall() {
        File jarfile = configuration.files(dependency).first()
        def of = new File(toolsFolder)
        of.mkdirs()
        project.copy {
            def cp = (CopySpec)it
            cp.from jarfile
            cp.into of
            cp.rename {
                toolName + ".jar"
            }
        }
        if (OperatingSystem.current().isWindows()) {
            extractScriptWindows()
        } else {
            extractScriptUnix()
        }
        def toolJson = new ToolJson()
        toolJson.name = toolName
        toolJson.version = dependency.version
        toolJson.cpp = false
        setToolVersion(toolJson)
    }

    private void extractScriptWindows() {

        ToolInstallTask.class.getResourceAsStream('/ScriptBase.vbs').withCloseable {
            def outputFile = new File(toolsFolder, toolName + '.vbs')
            outputFile.text = it.text
        }
    }

    private void extractScriptUnix() {
        def outputFile = new File(toolsFolder, toolName + '.py')
        ToolInstallTask.class.getResourceAsStream('/ScriptBase.py').withCloseable {
            outputFile.text = it.text
        }
        project.exec { ExecSpec spec ->
            spec.commandLine "chmod"
            spec.args("0755", outputFile.absolutePath)
        }
    }
}
