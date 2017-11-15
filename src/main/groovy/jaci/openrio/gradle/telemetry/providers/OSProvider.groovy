package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class OSProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def obj = new JsonObject()
        def os = OperatingSystem.current()
        obj.addProperty('name', os.name)
        obj.addProperty('family', os.familyName)
        obj.addProperty('version', os.version)
        obj.addProperty('nativePrefix', os.nativePrefix)
        return obj
    }
}
