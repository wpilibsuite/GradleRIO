package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project

@CompileStatic
class GradleProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        return new JsonPrimitive(project.gradle.gradleVersion)
    }
}
