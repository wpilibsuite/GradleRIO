package edu.wpi.first.gradlerio.test

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.jvm.tasks.Jar

@CompileStatic
class ExtractTestJNITask extends DefaultTask {
    @Internal
    Jar jar

    @TaskAction
    void extract() {
        // Extract necessary libs
        def nativeLibs = project.configurations.getByName('nativeDesktopLib')
        def nativeZips = project.configurations.getByName('nativeDesktopZip')
        FileCollection extractedFiles = null as FileCollection

        nativeLibs.dependencies
                .matching { Dependency dep -> dep != null && nativeLibs.files(dep).size() > 0 }
                .all { Dependency dep ->
                    def fc = project.files(nativeLibs.files(dep).toArray())
                    if (extractedFiles == null) extractedFiles = fc
                    else extractedFiles += fc
                }

        nativeZips.dependencies
                .matching { Dependency dep -> dep != null && nativeZips.files(dep).size() > 0 }
                .all { Dependency dep ->
                    def ziptree = project.zipTree(nativeZips.files(dep).first())
                    ["**/*.so*", "**/*.so", "**/*.dll", "**/*.dylib"].collect { String pattern ->
                        def fc = ziptree.matching { PatternFilterable pat -> pat.include(pattern) }
                        if (extractedFiles == null) extractedFiles = fc
                        else extractedFiles += fc
                    }
                }

        File dir = JavaTestPlugin.jniExtractionDir(project)
        if (dir.exists()) dir.deleteDir()
        dir.parentFile.mkdirs()

        if (extractedFiles != null) {
            project.copy { CopySpec s ->
                s.from(project.files { extractedFiles.files })
                s.into(dir)
            }
        }
    }
}
