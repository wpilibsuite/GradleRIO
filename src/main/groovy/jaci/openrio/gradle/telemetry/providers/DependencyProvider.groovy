package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.SelfResolvingDependency

/**
 * Lists project dependencies
 * This helps us determine library support for popular libraries
 */
@CompileStatic
class DependencyProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def obj = new JsonObject()
        project.configurations.each { Configuration config ->
            if (config.canBeResolved) {
                def arr = new JsonArray()
                config.dependencies.each { Dependency dep ->
                    if (dep instanceof SelfResolvingDependency) {
                        (dep as SelfResolvingDependency).resolve().each { File f ->
                            arr.add(new JsonPrimitive(f.name))
                        }
                    } else {
                        arr.add(new JsonPrimitive("${dep.group}:${dep.name}:${dep.version}".toString()))
                    }
                }
                obj.add(config.name, arr)
            }
        }
        return obj
    }
}
