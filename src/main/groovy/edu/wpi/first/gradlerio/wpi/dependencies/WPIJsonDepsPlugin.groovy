package edu.wpi.first.gradlerio.wpi.dependencies

import com.google.gson.Gson
import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class WPIJsonDepsPlugin implements Plugin<Project> {

    static class Artifact {
        String artifact
        String[] validClassifiers
    }

    static class JsonDependency {
        String name
        String uuid
        Artifact[] javaDependencies
        Artifact[] jniDependencies
        Artifact[] cppDependencies
    }

    JsonDependency[] dependencies

    @Override
    void apply(Project project) {
        def wpi = project.extensions.getByType(WPIExtension)

        // Try to load dependencies JSON file
        File jsonDepFile = project.file('dependencies.json')
        if (jsonDepFile.exists()) {

            jsonDepFile.withReader {
                dependencies = new Gson().fromJson(it, JsonDependency[])
            }
        }
    }
}
