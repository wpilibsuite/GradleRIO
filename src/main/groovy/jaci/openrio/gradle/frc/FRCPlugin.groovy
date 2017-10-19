package jaci.openrio.gradle.frc

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.ArtifactBase
import jaci.gradle.deploy.artifact.FileArtifact
import jaci.gradle.deploy.artifact.FileCollectionArtifact
import jaci.gradle.deploy.artifact.NativeLibraryArtifact
import jaci.openrio.gradle.ExportJarResourceTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.nativeplatform.DependentSourceSet
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinary
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.BinaryTasks

@CompileStatic
class FRCPlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.pluginManager.apply(EmbeddedTools)

        project.afterEvaluate {
            addNetconsoleArtifact(project)
            addNativeLibraryArtifacts(project)
        }
    }

    DeployExtension deployExtension(Project project) {
        return project.extensions.getByType(DeployExtension)
    }

    static void allRoborioTargets(DeployExtension ext, ArtifactBase artifact) {
        ext.targets.withType(RoboRIO).all { RoboRIO r ->
            artifact.targets << r.name
        }
    }

    void addNetconsoleArtifact(Project project) {
        ExportJarResourceTask netconsolehost_task = project.tasks.create('exportNetconsoleHost', ExportJarResourceTask) { ExportJarResourceTask task ->
            task.resource = "netconsole/netconsole-host"
            task.outfile = new File(project.buildDir, "gradlerio/resource/${task.resource}")
        }

        deployExtension(project).artifacts.fileArtifact('netconsole') { FileArtifact artifact ->
            artifact.dependsOn(netconsolehost_task)
            allRoborioTargets(deployExtension(project), artifact)
            artifact.predeploy << { DeployContext ctx -> ctx.execute('killall -q netconsole-host 2> /dev/null || :') }
            artifact.file = netconsolehost_task.outputs.files.first()
            artifact.directory = '/usr/local/frc/bin'
            artifact.filename = 'netconsole-host'
            artifact.postdeploy << { DeployContext ctx -> ctx.execute('chmod +x netconsole-host') }
        }
    }

    void addNativeLibraryArtifacts(Project project) {
        // Note: These include JNI. Actual native c/c++ is done through EmbeddedTools
        def nativeLibs = project.configurations.getByName('nativeLib')
        def nativeZips = project.configurations.getByName('nativeZip')

        deployExtension(project).artifacts.fileCollectionArtifact('nativeLibs') { FileCollectionArtifact artifact ->
            allRoborioTargets(deployExtension(project), artifact)
            artifact.files = project.files()
            artifact.directory = '/usr/local/frc/lib'
            artifact.postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }

            nativeLibs.dependencies.matching { Dependency dep -> dep != null && nativeLibs.files(dep).size() > 0 }.all { Dependency dep ->
                artifact.files = artifact.files + project.files(nativeLibs.files(dep).toArray())
            }
        }

        deployExtension(project).artifacts.fileCollectionArtifact('nativeZips') { FileCollectionArtifact artifact ->
            allRoborioTargets(deployExtension(project), artifact)
            artifact.files = project.files()
            artifact.directory = '/usr/local/frc/lib'
            artifact.postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }

            nativeZips.dependencies.matching { Dependency dep -> dep != null && nativeZips.files(dep).size() > 0 }.all { Dependency dep ->
                def ziptree = project.zipTree(nativeZips.files(dep).first())
                ["*.so*", "lib/*.so", "java/lib/*.so", "linux/athena/*.so"].collect { String pattern ->
                    artifact.files = artifact.files + ziptree.matching { PatternFilterable pat -> pat.include(pattern) }
                }
            }
        }
    }

    static class FRCRules extends RuleSource {
        @BinaryTasks
        void createNativeLibraryDeployTasks(final ModelMap<Task> tasks, final ExtensionContainer ext, final NativeBinarySpec binary) {
            def deployExt = ext.getByType(DeployExtension)
            def artifacts = deployExt.artifacts
            binary.inputs.withType(DependentSourceSet) { DependentSourceSet ss ->
                ss.libs.each { lss ->
                    if (lss instanceof LinkedHashMap) {
                        def lib = lss['library'] as String
                        if (artifacts.findByName(lib) == null) {
                            artifacts.nativeLibraryArtifact(lib) { NativeLibraryArtifact nla ->
                                FRCPlugin.allRoborioTargets(deployExt, nla)
                                nla.directory = '/usr/local/frc/lib'
                                nla.postdeploy << { DeployContext ctx -> ctx.execute('ldconfig') }
                                nla.library = lib
                            }
                        }
                    }
                }
            }
        }
    }

}
