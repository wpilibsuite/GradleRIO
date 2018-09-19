package edu.wpi.first.gradlerio.test.sim

import com.google.gson.GsonBuilder
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaExternalSimulationTask extends DefaultTask {
    @Internal
    List<Jar> jars = []

    @TaskAction
    void create() {
        def cfgs = []
        def extensions = SimulationPlugin.getHALExtensions(project)
        for (Jar jar : jars) {

            def manifestAttributes = jar.manifest.attributes

            if (!manifestAttributes.containsKey('Robot-Class')) {
                continue
            }
            def mainClass = manifestAttributes['Main-Class']
            def robotClass = manifestAttributes['Robot-Class']

            def libraryDir = TestPlugin.jniExtractionDir(project).absolutePath

            def cfg = [:]

            cfg['name'] = jar.baseName
            cfg['extensions'] = extensions
            cfg['librarydir'] = libraryDir
            cfg['mainclass'] = mainClass
            cfg['robotclass'] = robotClass
            cfgs << cfg
        }

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        def json = gbuilder.create().toJson(cfgs)

        println(json)

        def outfile = new File(project.buildDir, "debug/desktopinfo.json")
        outfile.parentFile.mkdirs()
        outfile.text = json
    }
}
