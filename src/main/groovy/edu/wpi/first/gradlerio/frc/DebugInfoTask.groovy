package edu.wpi.first.gradlerio.frc

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.Artifact
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class DebugInfoTask extends DefaultTask {
    @Internal
    List<Closure> extraArtifacts = []

    @OutputFile
    File outfile = new File(project.rootProject.buildDir, "${DebugInfoMergeTask.CONTAINER_FOLDER}/${project.name}.json")

    @TaskAction
    void writeDebugInfo() {
        def cfg = []
        FRCPlugin.deployExtension(project).artifacts.all { Artifact art ->
            if (art instanceof FRCJavaArtifact) {
                art.targets.all { String target ->
                    cfg << [
                        artifact: "${art.name} (in project ${project.name})".toString(),
                        target: target,
                        debugfile: "${art.name}_${target}.debugconfig".toString(),
                        project: project.name,
                        language: "java"
                    ]
                }
            } else if (art instanceof FRCNativeArtifact) {
                art.targets.all { String target ->
                    cfg << [
                        artifact: "${art.name} (in project ${project.name})".toString(),
                        target: target,
                        component: (art as FRCNativeArtifact).component,
                        debugfile: "${art.name}_${target}.debugconfig".toString(),
                        language: "cpp"
                    ]
                }
            } else {
                extraArtifacts.each { Closure toCall ->
                    toCall(art, cfg)
                }
            }
        }

        outfile.parentFile.mkdirs()

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        outfile.text = gbuilder.create().toJson(cfg)
    }

}
