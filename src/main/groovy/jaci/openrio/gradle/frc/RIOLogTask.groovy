package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset

@CompileStatic
class RIOLogTask extends DefaultTask {
    @TaskAction
    void riolog() {
        println "RIOLog Started! Use CTRL+C (SIGINT) to stop..."
        println ""
        if (project.gradle.startParameter.consoleOutput != ConsoleOutput.Plain) {
            println "NOTE: Recommended to use --console=plain with Riolog to disable output buffering"
            println "      run with `./gradlew riolog --console=plain`"
            System.out.flush()
        }
        def socket = new DatagramSocket(null as SocketAddress)
        socket.setReuseAddress(true)
        socket.setSoTimeout(1000)       // Just incase our thread gets interrupted by daemon, but we're still waiting on receive
        socket.bind(new InetSocketAddress(6666))

        def buf = new byte[4096]
        def packet = new DatagramPacket(buf, buf.length)

        while (!Thread.interrupted()) {     // In the case we're run in a daemon (like most times), this is a good thing to check
            try {
                socket.receive(packet)
                byte[] data = packet.getData()
                if (data != null) {
                    println("${new String(data, 0, packet.length, Charset.forName('UTF-8'))}")
                }
            } catch (SocketTimeoutException e) {  }
        }
    }
}
