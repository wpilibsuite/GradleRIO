package edu.wpi.first.gradlerio.test

import com.google.gson.GsonBuilder
import edu.wpi.first.gradlerio.test.JavaTestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaExternalSimulationTask extends DefaultTask {
    @TaskAction
    void create() {
        def cfgs = []
        def extensions = TestPlugin.getHALExtensions(project)
        for (Jar jar : taskDependencies.getDependencies(this).findAll { it instanceof Jar } as Set<Jar>) {
            def manifestAttributes = jar.manifest.attributes

            if (!manifestAttributes.containsKey('Robot-Class')) {
                continue
            }
            def mainClass = manifestAttributes['Main-Class']

            def libraryDir = JavaTestPlugin.jniExtractionDir(project).absolutePath

            def cfg = [:]

            cfg['name'] = jar.baseName
            cfg['file'] = jar.outputs.files.singleFile.absolutePath
            cfg['extensions'] = extensions
            cfg['librarydir'] = libraryDir
            cfg['mainclass'] = mainClass
            cfgs << cfg
        }

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        def json = gbuilder.create().toJson(cfgs)

        def outfile = new File(project.buildDir, "debug/desktopinfo.json")
        outfile.parentFile.mkdirs()
        outfile.text = json
    }
}
