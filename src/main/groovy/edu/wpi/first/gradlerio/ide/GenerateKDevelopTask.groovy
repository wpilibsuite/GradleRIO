package edu.wpi.first.gradlerio.ide

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.TaskAction
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.nativeplatform.NativeExecutableBinarySpec


import java.nio.file.Paths

@CompileStatic
class GenerateKDevelopTask extends DefaultTask {
    @TaskAction
    void generate() {
        def projectFile = new FileWriter(project.file("${project.name}.kdev4"))
        projectFile.write("""\
[Project]
Manager=KDevCustomBuildSystem
""")
        projectFile.write("Name=${project.name}")
        projectFile.close()

        def headerPaths = []
        def ext = project.extensions.getByType(EditorConfigurationExtension)
        ext._binaries.each { NativeExecutableBinarySpec bin ->
            bin.libs.each { NativeDependencySet ds ->
                headerPaths += ds.getIncludeRoots()
            }
        }
        headerPaths = headerPaths.unique()

        new File(".kdev4").mkdir()
        def buildSystemFile = new FileWriter(project.file(".kdev4/${project.name}.kdev4"))
        buildSystemFile.write("""\
[CustomBuildSystem]
CurrentConfiguration=BuildConfig0

[CustomBuildSystem][BuildConfig0]
BuildDir=
Title=FRC GradleRIO

[CustomBuildSystem][BuildConfig0][ToolBuild]
Arguments=build
Enabled=true
Executable=file:///${project.projectDir}/gradlew
Type=0

[CustomBuildSystem][BuildConfig0][ToolClean]
Arguments=clean
Enabled=true
Executable=file:///${project.projectDir}/gradlew
Type=3

[CustomBuildSystem][BuildConfig0][ToolInstall]
Arguments=deploy
Enabled=true
Executable=file:///${project.projectDir}/gradlew
Type=2

[CustomDefinesAndIncludes][ProjectPath0][Includes]
""")
        headerPaths.eachWithIndex {dir, index ->
            buildSystemFile.write("${index + 1}=${dir}\n")
        }
        buildSystemFile.close()
    }
}

class KDevelopPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(ComponentModelBasePlugin)
        project.extensions.create('KDevelop', EditorConfigurationExtension)

        project.tasks.register('KDevelop', GenerateKDevelopTask) { GenerateKDevelopTask task ->
            task.group = "GradleRIO"
            task.description = "Generate KDevelop4 Build Files."
        }
    }
}
