package edu.wpi.first.gradlerio.test

import com.google.gson.GsonBuilder
import edu.wpi.first.gradlerio.test.JavaTestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension

@CompileStatic
class JavaExternalSimulationTask extends ExternalSimulationTask {

    @OutputFile
    File outfile = new File(project.rootProject.buildDir, "${ExternalSimulationMergeTask.CONTAINER_FOLDER}/${project.name}_java.json")

    @TaskAction
    void create() {
        def cfgs = []
        SimulationExtension simExtension = project.extensions.getByType(SimulationExtension)
        def extensions = TestPlugin.getHALExtensions(project)
        for (Jar jar : taskDependencies.getDependencies(this).findAll { it instanceof Jar } as Set<Jar>) {
            def manifestAttributes = jar.manifest.attributes

            if (!manifestAttributes.containsKey('Main-Class')) {
                continue
            }
            def mainClass = manifestAttributes['Main-Class']

            def libraryDir = JavaTestPlugin.jniExtractionDir(project).absolutePath

            def cfg = [:]

            cfg['name'] = "${jar.baseName} (in project ${project.name})".toString()
            cfg['file'] = jar.outputs.files.singleFile.absolutePath
            cfg['extensions'] = extensions
            cfg['env'] = simExtension.environment
            cfg['librarydir'] = libraryDir
            cfg['mainclass'] = mainClass
            cfgs << cfg
        }

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        def json = gbuilder.create().toJson(cfgs)

        outfile.parentFile.mkdirs()
        outfile.text = json
    }
}
