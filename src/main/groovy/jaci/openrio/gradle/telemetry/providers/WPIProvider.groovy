package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Project

@CompileStatic
class WPIProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def obj = new JsonObject()
        def wpi = project.extensions.getByType(WPIExtension)
        wpi.versions().each { String name, Tuple tuple ->
            obj.addProperty(tuple[2] as String, tuple[1] as String)
        }
        return obj
    }
}
