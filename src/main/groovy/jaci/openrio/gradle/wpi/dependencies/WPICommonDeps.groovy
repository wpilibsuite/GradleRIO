package jaci.openrio.gradle.wpi.dependencies

import org.gradle.api.Plugin
import org.gradle.api.Project

class WPICommonDeps implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.repositories.maven { repo ->
            repo.name = "WPI"
            repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven { repo ->
            repo.name = "Jaci"
            repo.url = "http://dev.imjac.in/maven/"
        }
    }
}
