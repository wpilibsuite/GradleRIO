package jaci.openrio.gradle.deploy

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ExportFileResourceTask extends DefaultTask {
    @Input
    String resource

    @OutputFile
    File outfile

    @TaskAction
    def doExport() {
        def instream = DeployPlugin.class.getClassLoader().getResourceAsStream(resource)
        outfile.parentFile.mkdirs()
        outfile << instream

    }
}
