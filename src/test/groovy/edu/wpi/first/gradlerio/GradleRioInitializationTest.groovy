package edu.wpi.first.gradlerio

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class GradleRioInitializationTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "Cpp Project Initializes Correctly"() {
        given:
        buildFile << """
plugins {
    id 'cpp'
    id 'edu.wpi.first.GradleRIO'
}
"""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }

    def "Java Project Initializes Correctly"() {
        given:
        buildFile << """
plugins {
    id 'java'
    id 'edu.wpi.first.GradleRIO'
}
"""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }
}
