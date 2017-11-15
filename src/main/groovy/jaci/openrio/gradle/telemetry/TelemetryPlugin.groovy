package jaci.openrio.gradle.telemetry

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.telemetry.providers.*
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.zip.GZIPOutputStream

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

        def thread = null as Thread
        project.afterEvaluate {
            if (project.extensions.getByType(TelemetryExtension).reportTelemetry && !project.gradle.startParameter.isOffline() && (System.getenv('CI') == null || !System.getenv('CI').toBoolean())) {
                thread = new Thread({
                    def telemetry = renderTelemetry(project, false)
                    def reportFile = new File(GradleRIOPlugin.globalDirectory, 'lastreport.telemetry')
                    if (!reportFile.exists() || reportFile.text != telemetry || project.hasProperty('dirty-telemetry')) {
                        // Report telemetry to web
                        def baos = new ByteArrayOutputStream()
                        def gzstr = new GZIPOutputStream(baos)
                        gzstr.write(telemetry.bytes)
                        gzstr.close()

                        def b64 = baos.toByteArray().encodeBase64().toString()
                        baos.close()

                        try {
                            def url = new URL("http://openrio.imjac.in/gradlerio/telemetry/report")
                            url.openConnection().with { URLConnection conn ->
                                def http = conn as HttpURLConnection
                                http.doOutput = true
                                http.requestMethod = 'POST'
                                http.outputStream.withWriter { writer ->
                                    writer << b64
                                }
                                http.content
                            }
                            reportFile.text = telemetry
                        } catch (Exception e) { }
                    }
                })
                thread.start()
            }
        }

        project.gradle.buildFinished {
            thread.join()
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
