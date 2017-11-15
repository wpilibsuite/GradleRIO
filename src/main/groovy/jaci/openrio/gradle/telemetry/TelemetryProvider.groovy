package jaci.openrio.gradle.telemetry

import com.google.gson.JsonElement
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
interface TelemetryProvider {
    JsonElement telemetry(Project project)
}