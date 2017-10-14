package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ExportJarResourceTask extends DefaultTask {
    @Input
    String resource

    @OutputFile
    File outfile

    @TaskAction
    def doExport() {
        def instream = FRCPlugin.class.getClassLoader().getResourceAsStream(resource)
        outfile.parentFile.mkdirs()
        outfile << instream
    }
}
