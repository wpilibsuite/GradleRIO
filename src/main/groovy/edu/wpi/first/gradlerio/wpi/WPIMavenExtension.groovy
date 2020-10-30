package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

import javax.inject.Inject

@CompileStatic
class WPIMavenExtension extends DefaultNamedDomainObjectSet<WPIMavenRepo> {

    final Project project

    boolean useDevelopment
    boolean useLocal
    boolean useFrcMavenLocalDevelopment
    boolean useFrcMavenLocalRelease
    boolean useMavenCentral

    @Inject
    WPIMavenExtension(Project project) {
        super(WPIMavenRepo.class, DirectInstantiator.INSTANCE)
        this.project = project

        this.useDevelopment = false // Do not rename without changing versionupdates.gradle
        this.useLocal = true
        this.useFrcMavenLocalDevelopment = false
        this.useFrcMavenLocalRelease = false
        this.useMavenCentral = true

        mirror("Official") { WPIMavenRepo mirror ->
            mirror.release = "https://frcmaven.wpi.edu/artifactory/release"
            mirror.development = "https://frcmaven.wpi.edu/artifactory/development"
            mirror.priority = WPIMavenRepo.PRIORITY_OFFICIAL
        }

        // mirror("AU") { WPIMavenRepo mirror ->
        //     mirror.release = "http://wpimirror.imjac.in/m2/release"
        //     mirror.development = "http://wpimirror.imjac.in/m2/development"
        // }
    }

    // Mirror = source for WPILib artifacts
    // Repo = source for any artifacts

    // Repo should always take precedence over mirror in the case they want
    // to provide custom builds of WPILib artifacts.

    WPIMavenRepo mirror(String name, final Closure config) {
        def mirr = new WPIMavenRepo(name)
        mirr.priority = WPIMavenRepo.PRIORITY_MIRROR
        project.configure(mirr, config)
        this << (mirr)
        return mirr
    }

    WPIMavenRepo repo(String name, final Closure config) {
        def mirr = new WPIMavenRepo(name)
        project.configure(mirr, config)
        this << (mirr)
        return mirr
    }

    WPIMavenRepo vendor(String name, final Closure config) {
        def mirr = new WPIMavenRepo(name)
        mirr.priority = WPIMavenRepo.PRIORITY_VENDOR
        project.configure(mirr, config)
        this << (mirr)
        return mirr
    }

    void useMirror(String name) {
        all { WPIMavenRepo m ->
            if (m.name == name)
                m.priority = WPIMavenRepo.PRIORITY_MIRROR_INUSE
        }
    }

    void useRepo(String name) {
        all { WPIMavenRepo m ->
            if (m.name == name)
                m.priority = WPIMavenRepo.PRIORITY_REPO_INUSE
        }
    }
}
