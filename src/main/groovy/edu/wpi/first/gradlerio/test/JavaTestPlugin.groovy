package edu.wpi.first.gradlerio.test

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogging
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JavaTestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.tasks.register("extractTestJNI", ExtractTestJNITask, { ExtractTestJNITask t ->
            t.group = "GradleRIO"
            t.description = "Extract Test JNI Native Libraries (nativeDesktopLib, nativeDesktopZip)"
        } as Action<ExtractTestJNITask>)

        project.tasks.register("simulateExternalJava", JavaExternalSimulationTask, { JavaExternalSimulationTask task ->
            task.group = "GradleRIO"
            task.description = "Simulate External Task for Java/Kotlin/JVM. Exports a JSON file for use by editors / tools"

            task.dependsOn("extractTestJNI")
            task.dependsOn(project.tasks.withType(Jar))
            task.finalizedBy(project.tasks.withType(ExternalSimulationMergeTask))
        } as Action<JavaExternalSimulationTask>)

        project.tasks.register("simulateJava", JavaSimulationTask, { JavaSimulationTask task ->
            task.group = "GradleRIO"
            task.description = "Launch simulation for Java/Kotlin/JVM"

            task.dependsOn("extractTestJNI")
            task.dependsOn(project.tasks.withType(Jar))
        } as Action<JavaSimulationTask>)

        // Java Unit Tests
        project.tasks.withType(Test).configureEach { Test t ->
            t.dependsOn("extractTestJNI")

            t.doFirst {
                def env = [:] as Map<String, String>

                def ldpath = jniExtractionDir(project).absolutePath

                if (OperatingSystem.current().isUnix()) {
                    env["LD_LIBRARY_PATH"] = ldpath
                    env["DYLD_FALLBACK_LIBRARY_PATH"] = ldpath
                    env["DYLD_LIBRARY_PATH"] = ldpath
                } else if (OperatingSystem.current().isWindows()) {
                    env["PATH"] = ldpath + TestPlugin.envDelimiter() + System.getenv("PATH")
                }

                t.environment(env)

                def jlp = ldpath
                if (t.systemProperties.containsKey('java.library.path'))
                    jlp = (String)t.systemProperties.get('java.library.path') + TestPlugin.envDelimiter() + ldpath
                t.systemProperties.put('java.library.path', jlp)
            }

            t.testLogging { TestLogging log ->
                log.events(TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_ERROR,
                        TestLogEvent.STANDARD_OUT)
                log.showStandardStreams = true
            }
        }
    }

    static File jniExtractionDir(Project project) {
        return new File(project.buildDir, "tmp/jniExtractDir")
    }
}
