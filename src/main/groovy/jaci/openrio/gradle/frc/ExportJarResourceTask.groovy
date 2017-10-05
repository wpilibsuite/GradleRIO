package jaci.openrio.gradle.frc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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
