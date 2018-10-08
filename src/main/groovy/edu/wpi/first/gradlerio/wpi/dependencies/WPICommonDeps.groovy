package edu.wpi.first.gradlerio.wpi.dependencies

import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.WPIMavenRepo
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
            def wpi = project.extensions.getByType(WPIExtension)

            if (wpi.maven.useLocal) {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "WPILocal"
                    repo.url = "${project.extensions.getByType(WPIExtension).getFrcHome()}/maven"
                }
            }

            if (wpi.maven.useFrcMavenLocalDevelopment) {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "FRCDevelopmentLocal"
                    repo.url = "${System.getProperty('user.home')}/releases/maven/development"
                }
            }

            if (wpi.maven.useFrcMavenLocalRelease) {
                project.repositories.maven { MavenArtifactRepository repo ->
                    repo.name = "FRCReleaseLocal"
                    repo.url = "${System.getProperty('user.home')}/releases/maven/release"
                }
            }

            def sortedMirrors = wpi.maven.sort { it.priority }

            // If enabled, the development branch should have a higher weight than the release
            // branch.
            if (wpi.maven.useDevelopment) {
                sortedMirrors.each { WPIMavenRepo mirror ->
                    if (mirror.development != null)
                        project.repositories.maven { MavenArtifactRepository repo ->
                            repo.name = "WPI${mirror.name}Development"
                            repo.url = mirror.development
                        }
                }
            }

            sortedMirrors.each { WPIMavenRepo mirror ->
                if (mirror.release != null)
                    project.repositories.maven { MavenArtifactRepository repo ->
                        repo.name = "WPI${mirror.name}Release"
                        repo.url = mirror.release
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
