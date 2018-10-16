package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import edu.wpi.first.gradlerio.wpi.WPIPlugin
import edu.wpi.first.gradlerio.wpi.WPIExtension
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.Project

class WPIExtensionExtendableTest extends Specification {

    def outerName = ''

    def "can extend wpi extension"() {
        outerName = ''
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(WPIPlugin)
        def wpi = (WPIExtension)project.extensions.getByType(WPIExtension)
        wpi.extensions.add('testAdd', { String name ->
            outerName = name
        })

        when:
            def test = wpi.extensions.getByName('testAdd')
            test('hello')
        then:
            outerName == 'hello'
    }

    @CompileStatic
    def "can extend wpi extension static"() {
        outerName = ''
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(WPIPlugin)
        def wpi = project.extensions.getByType(WPIExtension) as ExtensionAware
        wpi.extensions.add('testAdd', { String name ->
            outerName = name
        })

        when:
            def test = wpi.extensions.getByName('testAdd') as Closure
            test('hello')
        then:
            outerName == 'hello'
    }
}
