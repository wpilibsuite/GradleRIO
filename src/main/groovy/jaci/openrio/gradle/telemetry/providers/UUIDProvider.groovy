package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project

/**
 * Reports a unique, anonymous UUID for each user (helps us determine 'unique users')
 */
@CompileStatic
class UUIDProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def uuidFile = new File(GradleRIOPlugin.globalDirectory, 'user.uuid')
        def uuid = UUID.randomUUID().toString()
        if (uuidFile.exists()) {
            uuid = uuidFile.text
        } else {
            uuidFile.text = uuid
        }
        return new JsonPrimitive(uuid)
    }
}
