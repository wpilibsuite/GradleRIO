package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.ExternalLaunchTask
import edu.wpi.first.gradlerio.test.JavaTestPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaSimulationTask extends ExternalLaunchTask {
    @TaskAction
    void run() {
        def ldpath = JavaTestPlugin.jniExtractionDir(project).absolutePath
        def java = Jvm.current().getExecutable("java").absolutePath
        environment = TestPlugin.getSimLaunchEnv(project, ldpath)
        for (Jar jar : taskDependencies.getDependencies(this).findAll { it instanceof Jar } as Set<Jar>) {
            def manifestAttributes = jar.manifest.attributes

            if (!manifestAttributes.containsKey('Main-Class')) {
                continue
            }

            if (OperatingSystem.current().isMacOsX()) {
                launch("\"$java\"", "-Djava.library.path=\"$ldpath\"", "-XstartOnFirstThread", "-jar", "\"${jar.archivePath.toString()}\"")
            } else {
                launch("\"$java\"", "-Djava.library.path=\"$ldpath\"", "-jar", "\"${jar.archivePath.toString()}\"")
            }
            return
        }

        def logger = ETLoggerFactory.INSTANCE.create("JavaSimulation")
        logger.logError("Failed to find a Jar file with a Main-Class attribute")
    }
}
