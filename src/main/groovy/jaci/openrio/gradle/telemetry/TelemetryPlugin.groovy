package jaci.openrio.gradle.telemetry

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.openrio.gradle.telemetry.providers.*
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin reports project statistics to OpenRIO Web Services to help with
 * further development.
 *
 * This plugin's actions can be disabled if you're paranoid.
 */
@CompileStatic
class TelemetryPlugin implements Plugin<Project> {

    Map<String, TelemetryProvider> providers = [:];

    @Override
    void apply(Project project) {
        project.extensions.create("telemetry", TelemetryExtension)

        register('uuid', new UUIDProvider())
        register('os', new OSProvider())
        register('gradle', new GradleProvider())
        register('plugins', new PluginsProvider())
        register('classpath', new ClasspathProvider())
        register('deploy', new DeployProvider())
        register('dependencies', new DependencyProvider())
        register('wpi', new WPIProvider())

        project.tasks.create('telemetry') { DefaultTask task ->
            task.group = 'GradleRIO'
            task.description = 'Render the collected telemetry to the console'

            task.doLast {
                println renderTelemetry(project, true)
            }
        }
    }

    JsonObject telemetryReport(Project project) {
        def obj = new JsonObject()
        providers.each { String name, TelemetryProvider prov ->
            obj.add(name, prov.telemetry(project))
        }
        return obj
    }

    String renderTelemetry(Project project, boolean pretty) {
        def builder = new GsonBuilder()
        if (pretty) builder.setPrettyPrinting()
        return builder.create().toJson(telemetryReport(project))
    }

    void register(String name, TelemetryProvider provider) {
        providers[name] = provider
    }

}
