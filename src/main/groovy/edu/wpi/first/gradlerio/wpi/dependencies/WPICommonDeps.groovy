package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

@CompileStatic
class WPICommonDeps implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            if (!project.hasProperty("wpi-no-local-maven")) {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "WPILocal"
                    repo.url = "${project.extensions.getByType(WPIExtension).getFrcHome()}/maven"
                }
            }

            def wpi = project.extensions.getByType(WPIExtension)

            if (wpi.developmentBranch) {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "WPI"
                    repo.url = "http://first.wpi.edu/FRC/roborio/maven/development"
                }
            } else {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "WPI"
                    repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
                }
            }
        }

        apply_halsim_extensions(project, project.extensions.getByType(WPIExtension))
    }

    @CompileDynamic
    void apply_halsim_extensions(Project project, WPIExtension wpi) {
        def nativeclassifier = wpi.nativeClassifier

        if (nativeclassifier != null) {
            project.dependencies.ext.sim = [
                print: {
                    ["edu.wpi.first.halsim:halsim-print:${wpi.wpilibVersion}:${nativeclassifier}@zip"]
                },
                nt_ds: {
                    ["edu.wpi.first.halsim.ds:halsim-ds-nt:${wpi.wpilibVersion}:${nativeclassifier}@zip"]
                },
                nt_readout: {
                    ["edu.wpi.first.halsim:halsim-lowfi:${wpi.wpilibVersion}:${nativeclassifier}@zip"]
                }
            ]
        }
    }
}
