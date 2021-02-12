package edu.wpi.first.gradlerio.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.GsonBuilder;
import edu.wpi.first.gradlerio.test.JavaTestPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

public class JavaExternalSimulationTask extends ExternalSimulationTask {
    private RegularFileProperty outfile;// = new File(project.rootProject.buildDir, "${ExternalSimulationMergeTask.CONTAINER_FOLDER}/${project.name}_java.json")

    @OutputFile
    public RegularFileProperty getOutfile() {
        return outfile;
    }

    @Inject
    public JavaExternalSimulationTask(ObjectFactory objects) {
        outfile = objects.fileProperty();
        outfile.set(new File(getProject().getRootProject().getBuildDir(), ExternalSimulationMergeTask.CONTAINER_FOLDER + "/" + getProject().getName() + "_java.json"));
    }

    @TaskAction
    public void create() {
        // List<Object> cfgs = new ArrayList<Object>();
        // SimulationExtension simExtension = getProject().getExtensions().getByType(SimulationExtension.class)
        // def extensions = TestPlugin.getHALExtensions(project)
        // for (Jar jar : taskDependencies.getDependencies(this).findAll { it instanceof Jar } as Set<Jar>) {
        //     def manifestAttributes = jar.manifest.attributes

        //     if (!manifestAttributes.containsKey('Main-Class')) {
        //         continue
        //     }
        //     def mainClass = manifestAttributes['Main-Class']

        //     def libraryDir = JavaTestPlugin.jniExtractionDir(project).absolutePath

        //     def cfg = [:]

        //     cfg['name'] = "${jar.baseName} (in project ${project.name})".toString()
        //     cfg['file'] = jar.outputs.files.singleFile.absolutePath
        //     cfg['extensions'] = extensions
        //     cfg['env'] = simExtension.environment
        //     cfg['librarydir'] = libraryDir
        //     cfg['mainclass'] = mainClass
        //     cfgs << cfg
        // }

        // def gbuilder = new GsonBuilder()
        // gbuilder.setPrettyPrinting()
        // def json = gbuilder.create().toJson(cfgs)

        // outfile.parentFile.mkdirs()
        // outfile.text = json
    }
}
