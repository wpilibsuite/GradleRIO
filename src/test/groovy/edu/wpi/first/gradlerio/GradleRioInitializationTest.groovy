package edu.wpi.first.gradlerio

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class GradleRioInitializationTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File buildFile2
    File settingsFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        testProjectDir.newFolder('sub')
        buildFile2 = testProjectDir.newFile('sub/build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    def "Cpp Project Initializes Correctly"() {
        given:
        buildFile << """
plugins {
    id 'cpp'
    id 'edu.wpi.first.GradleRIO'
}
"""
        buildFile2 << ""
        settingsFile << ""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks', '--stacktrace')
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
        buildFile2 << ""
        settingsFile << ""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }

    def "Cpp SubProject Initializes Correctly"() {
        given:
        buildFile2 << """
plugins {
    id 'cpp'
    id 'edu.wpi.first.GradleRIO'
}
"""
        buildFile << ""
        settingsFile << "include 'sub'"
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }

    def "Java SubProject Initializes Correctly"() {
        given:
        buildFile2 << """
plugins {
    id 'java'
    id 'edu.wpi.first.GradleRIO'
}
"""
        buildFile << ""
        settingsFile << "include 'sub'"
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tasks', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }
}
