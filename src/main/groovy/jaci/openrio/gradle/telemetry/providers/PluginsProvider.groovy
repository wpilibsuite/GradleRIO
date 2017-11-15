package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class PluginsProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def arr = new JsonArray()
        project.plugins.each { Plugin plugin ->
            arr.add(new JsonPrimitive(plugin.class.name))
        }
        return arr
    }
}
