package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.ExternalLaunchTask
import edu.wpi.first.gradlerio.test.JavaTestPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaSimulationTask extends ExternalLaunchTask {
    @TaskAction
    void run() {
        def ldpath = JavaTestPlugin.jniExtractionDir(project).absolutePath
        def java = Jvm.current().getExecutable("java").absolutePath
        def jar = taskDependencies.getDependencies(this).find { it instanceof Jar } as Jar
        environment = TestPlugin.getSimLaunchEnv(project, ldpath)

        launch("\"$java\"", "-Djava.library.path=\"$ldpath\"", "-jar", "\"${jar.archivePath.toString()}\"")
    }
}
