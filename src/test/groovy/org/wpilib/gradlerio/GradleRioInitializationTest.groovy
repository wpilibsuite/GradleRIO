package org.wpilib.gradlerio

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import spock.lang.TempDir
import spock.lang.Specification

class GradleRioInitializationTest extends Specification {
    @TempDir File testProjectDir
    File buildFile
    File buildFile2
    File settingsFile

    def setup() {
        buildFile = new File(testProjectDir, 'build.gradle')
        new File(testProjectDir, 'sub').mkdirs()
        buildFile2 = new File(testProjectDir, 'sub/build.gradle')
        settingsFile = new File(testProjectDir, 'settings.gradle')
    }

    def "Cpp Project Initializes Correctly"() {
        given:
        buildFile << """
plugins {
    id 'cpp'
    id 'org.wpilib.GradleRIO'
}
"""
        buildFile2 << ""
        settingsFile << ""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
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
    id 'org.wpilib.GradleRIO'
}
"""
        buildFile2 << ""
        settingsFile << ""
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
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
    id 'org.wpilib.GradleRIO'
}
"""
        buildFile << ""
        settingsFile << "include 'sub'"
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
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
    id 'org.wpilib.GradleRIO'
}
"""
        buildFile << ""
        settingsFile << "include 'sub'"
        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('tasks', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(':tasks').outcome == SUCCESS
    }
}
