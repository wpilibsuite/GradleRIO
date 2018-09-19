package edu.wpi.first.gradlerio.frc

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.Artifact
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class DebugInfoTask extends DefaultTask {

    @TaskAction
    void writeDebugInfo() {
        def cfg = []
        FRCPlugin.deployExtension(project).artifacts.all { Artifact art ->
            if (art instanceof FRCJavaArtifact) {
                art.targets.all { String target ->
                    cfg << [
                        artifact: art.name,
                        target: target,
                        debugfile: "${art.name}_${target}.debugconfig".toString(),
                        project: project.name,
                        language: "java"
                    ]
                }
            } else if (art instanceof FRCNativeArtifact) {
                art.targets.all { String target ->
                    cfg << [
                        artifact: art.name,
                        target: target,
                        component: (art as FRCNativeArtifact).component,
                        debugfile: "${art.name}_${target}.debugconfig".toString(),
                        language: "cpp"
                    ]
                }
            }
        }

        def file = new File(project.buildDir, "debug/debuginfo.json")
        file.parentFile.mkdirs()

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        file.text = gbuilder.create().toJson(cfg)
    }

}
