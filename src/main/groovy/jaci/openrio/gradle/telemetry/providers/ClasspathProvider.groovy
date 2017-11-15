package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

/**
 * Lists resolved classpath dependencies
 * These are the plugins as well as their versions (lets us tell what the gradlerio version is)
 */
@CompileStatic
class ClasspathProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def arr = new JsonArray()
        project.buildscript.configurations.getByName('classpath').resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency dep ->
            arr.add(new JsonPrimitive(dep.name))
        }
        return arr
    }
}
