package edu.wpi.first.gradlerio.test

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.ExternalLaunchTask
import edu.wpi.first.gradlerio.test.JavaTestPlugin
import edu.wpi.first.gradlerio.test.TestPlugin
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar
import java.nio.file.Paths

@CompileStatic
class JavaSimulationTask extends ExternalLaunchTask {
    @TaskAction
    void run() {
        def ldpath = JavaTestPlugin.jniExtractionDir(project).absolutePath

        def javaFile = Jvm.current().getExecutable("java")
        def java = javaFile.absolutePath
        def javaFileName = javaFile.name

        // Search for WPILib JDK first
        def frcHome = project.extensions.getByType(WPIExtension).frcHome
        //C:\Users\Public\wpilib\2021\jdk\bin
        def jdkPath = Paths.get(frcHome, "jdk", "bin").toFile()
        if (jdkPath.exists()) {
            java = (new File(jdkPath, javaFileName)).absolutePath.toString()
        }

        environment.putAll(TestPlugin.getSimLaunchEnv(project, ldpath))
        for (Jar jar : taskDependencies.getDependencies(this).findAll { it instanceof Jar } as Set<Jar>) {
            def manifestAttributes = jar.manifest.attributes

            if (!manifestAttributes.containsKey('Main-Class')) {
                continue
            }

            if (OperatingSystem.current().isMacOsX()) {
                launch("\"$java\"", "-Djava.library.path=\"$ldpath\"", "-XstartOnFirstThread", "-jar", "\"${jar.archivePath.toString()}\"")
            } else {
                if (OperatingSystem.current().isWindows()) {
                    println "If you receive errors loading the JNI dependencies, make sure you have the latest Visual Studio C++ Redstributable installed."
                    println "That can be found at https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads"
                }
                launch("\"$java\"", "-Djava.library.path=\"$ldpath\"", "-jar", "\"${jar.archivePath.toString()}\"")
            }
            return
        }

        def logger = ETLoggerFactory.INSTANCE.create("JavaSimulation")
        logger.logError("Failed to find a Jar file with a Main-Class attribute")
    }
}
