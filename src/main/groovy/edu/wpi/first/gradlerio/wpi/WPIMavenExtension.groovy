package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

@CompileStatic
class WPIMavenExtension extends DefaultNamedDomainObjectSet<WPIMirror> {

    final Project project

    boolean useDevelopment
    boolean useLocal

    WPIMavenExtension(Project project) {
        super(WPIMirror.class, DirectInstantiator.INSTANCE)
        this.project = project

        this.useDevelopment = true
        this.useLocal = true

        mirror("Official") { WPIMirror mirror ->
            mirror.release = "http://first.wpi.edu/FRC/roborio/maven/release"
            mirror.development = "http://first.wpi.edu/FRC/roborio/maven/development"
            mirror.priority = 150
        }

        mirror("AU") { WPIMirror mirror ->
            mirror.release = "http://wpimirror.imjac.in/m2/release"
            mirror.development = "http://wpimirror.imjac.in/m2/development"
            mirror.priority = 200
        }
    }

    WPIMirror mirror(String name, final Closure config) {
        def mirr = new WPIMirror(name);
        project.configure(mirr, config)
        this << (mirr)
        return mirr
    }

    void useMirror(String name) {
        all { WPIMirror m ->
            if (m.name == name)
                m.priority = 120
        }
    }
}
