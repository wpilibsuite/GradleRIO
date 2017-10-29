package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import jaci.openrio.gradle.frc.riolog.RiologConnection
import org.gradle.api.DefaultTask
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction

import java.nio.ByteBuffer
import java.nio.charset.Charset

@CompileStatic
class RIOLogTask extends DefaultTask {
    @TaskAction
    void riolog() {
        println "RIOLog Started! Use CTRL+C (SIGINT) to stop..."
        println "Remember, the Driver Station must be connected for RIOLog to work!"
        println ""
        if (project.gradle.startParameter.consoleOutput != ConsoleOutput.Plain) {
            println "NOTE: Recommended to use --console=plain with Riolog to disable output buffering"
            println "      run with `./gradlew riolog --console=plain`"
            System.out.flush()
        }

        def discoveries = dependsOn.findAll {
            i -> i instanceof TargetDiscoveryTask && (i as TargetDiscoveryTask).isTargetActive()
        }.collect {
            it as TargetDiscoveryTask
        }

        def hosts = discoveries.collect() { TargetDiscoveryTask discover ->
            discover.context.selectedHost()
        }
        if (hosts.empty) throw new StopExecutionException()
        def host = hosts.first()

        def conn = new RiologConnection(host)
        conn.start()
        while (!Thread.currentThread().interrupted()) {
            try {
                conn.join()
            } catch (InterruptedException e) {
                println "Interrupted!"
                Thread.currentThread().interrupt()
                conn.interrupt()
            }
        }
    }
}
