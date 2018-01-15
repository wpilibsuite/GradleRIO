package jaci.openrio.gradle.telemetry

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.telemetry.providers.*
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.LoggerFactory

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
        project.gradle.buildFinished {
            def log = LoggerFactory.getLogger('gradlerio_telemetry')

            def noTelemetry = project.hasProperty('no-telemetry')
            def reportConfig = project.extensions.getByType(TelemetryExtension).reportTelemetry
            def offline = project.gradle.startParameter.isOffline()
            def CI = !(System.getenv('CI') == null)

            if (!noTelemetry && reportConfig && !offline && !CI) {
                thread = new Thread({
                    def start = System.currentTimeMillis()
                    try {
                        def telemetry = renderTelemetry(project, false)
                        def reportFile = new File(GradleRIOPlugin.globalDirectory, 'lastreport.telemetry')
                        // Report telemetry to web
                        def baos = new ByteArrayOutputStream()
                        def gzstr = new GZIPOutputStream(baos)
                        gzstr.write(telemetry.bytes)
                        gzstr.close()

                        def b64 = baos.toByteArray().encodeBase64().toString()
                        baos.close()

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

                    } catch (Exception e) {
                        def s = new StringWriter()
                        def pw = new PrintWriter(s)
                        e.printStackTrace(pw)
                        log.info("Could not run upload Telemetry...")
                        log.info(s.toString())
                    }
                    log.info("Telemetry Report took ${System.currentTimeMillis() - start}ms")
                })
                thread.start()
            } else {
                log.info("Telemetry skipped! (no-telemetry = ${noTelemetry}, ReportTelemetry = ${reportConfig}, offline = ${offline}, CI = ${CI}) (expected false, true, false, false)")
            }
        }

        project.gradle.buildFinished {
            if (thread != null) thread.join()
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
