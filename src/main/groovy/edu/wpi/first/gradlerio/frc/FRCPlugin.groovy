package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.frc.riolog.RiologPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.ActionWrapper
import jaci.gradle.deploy.artifact.ArtifactDeployTask
import jaci.gradle.deploy.artifact.CommandArtifact
import jaci.gradle.deploy.artifact.JavaArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.BinaryTasks

import java.util.function.Function

@CompileStatic
class FRCPlugin implements Plugin<Project> {

    Project project

    public static final String LIB_DEPLOY_DIR = '/usr/local/frc/third-party/lib'

    @Override
    void apply(Project project) {
        this.project = project
        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(RiologPlugin)

        def debugInfoLazy = project.tasks.register("writeDebugInfo", DebugInfoTask, { DebugInfoTask t ->
            t.finalizedBy(project.tasks.withType(DebugInfoMergeTask))
        } as Action<DebugInfoTask>)
        def frcExt = project.extensions.create("frc", FRCExtension, project)

        project.tasks.register("mergeDebugInfo", DebugInfoMergeTask, { DebugInfoMergeTask t ->
            t.dependsOn(debugInfoLazy)
        } as Action<DebugInfoMergeTask>)

        project.tasks.withType(ArtifactDeployTask).configureEach { ArtifactDeployTask t ->
            t.dependsOn(debugInfoLazy)
        }

        project.afterEvaluate {
            addNativeLibraryArtifacts(project)
            addJreArtifact(project)
            addCommandArtifacts(project)
        }

        def deployExtension = project.extensions.getByType(DeployExtension)
        def artifactExtensionAware = deployExtension.artifacts as ExtensionAware
        def targetExtensionAware = deployExtension.targets as ExtensionAware
        def artifactExtension = deployExtension.artifacts
        def targetExtension = deployExtension.targets

        artifactExtensionAware.extensions.add('frcJavaArtifact', { String name, Closure closure->
            return artifactExtension.artifact(name, FRCJavaArtifact, new ActionWrapper(closure))
        })

        artifactExtensionAware.extensions.add('frcNativeArtifact', { String name, Closure closure->
            return artifactExtension.artifact(name, FRCNativeArtifact, new ActionWrapper(closure))
        })

        artifactExtensionAware.extensions.add('frcNativeLibraryArtifact', { String name, Closure closure->
            return artifactExtension.artifact(name, FRCNativeLibraryArtifact, new ActionWrapper(closure))
        })

        targetExtensionAware.extensions.add('roboRIO', { String name, Closure closure ->
            targetExtension.target(name, RoboRIO, new ActionWrapper(closure))
        })

        targetExtensionAware.extensions.add('frcCompatibleTarget', { String name, Closure closure ->
            targetExtension.target(name, FRCCompatibleTarget, new ActionWrapper(closure))
        })
    }

    public static void ownDirectory(DeployContext ctx, String directory) {
        ctx.execute("chmod -R 777 \"$directory\" || true; chown -R lvuser:ni \"$directory\"")
    }

    public static DeployExtension deployExtension(Project project) {
        return project.extensions.getByType(DeployExtension)
    }

    public static void allFrcTargets(DeployExtension ext, Artifact artifact) {
        ext.targets.withType(FRCCompatibleTarget).all { RoboRIO r ->
            artifact.targets << r.name
        }
    }

    void addCommandArtifacts(Project project) {
        // As far as I can tell, the order of this doesn't matter
        // It only comments out some stuff for the LabVIEW runtime that apparently isn't needed, and dramatically
        // reduces memory usage.
        // See https://github.com/wpilibsuite/EclipsePlugins/pull/154
        deployExtension(project).artifacts.commandArtifact('roborioCommands') { CommandArtifact artifact ->
            allFrcTargets(deployExtension(project), artifact)
            artifact.command = "sed -i -e 's/^StartupDLLs/;StartupDLLs/' /etc/natinst/share/ni-rt.ini"
        }
    }

    void addJreArtifact(Project project) {
        def dext = deployExtension(project)
        dext.artifacts.artifact('jre', FRCJREArtifact) { FRCJREArtifact artifact ->
            allFrcTargets(dext, artifact)
            artifact.buildRequiresJre = { DeployContext ctx ->
                dext.artifacts.withType(JavaArtifact).size() > 0 || project.hasProperty("deploy-force-jre")
            } as Function<DeployContext, Boolean>
        }
    }

    void addNativeLibraryArtifacts(Project project) {
        // Note: These include JNI. Actual native c/c++ is done through EmbeddedTools
        def nativeLibs = project.configurations.getByName('nativeLib')
        def nativeZips = project.configurations.getByName('nativeZip')

        def dext = deployExtension(project)
        dext.artifacts.artifact('nativeLibs', ConfigurationArtifact) { ConfigurationArtifact artifact ->
            allFrcTargets(dext, artifact)
            artifact.configuration = nativeLibs
            artifact.zipped = false
        }

        dext.artifacts.artifact('nativeZip', ConfigurationArtifact) { ConfigurationArtifact artifact ->
            allFrcTargets(dext, artifact)
            artifact.configuration = nativeZips
            artifact.zipped = true
            artifact.filter = { PatternFilterable pat ->
                pat.include('*.so*', 'lib/*.so', 'java/lib/*.so', 'linux/athena/shared/*.so', 'linuxathena/**/*.so', '**/libopencv*.so.*')
            }
        }
    }

    static class FRCRules extends RuleSource {
        @BinaryTasks
        void createNativeLibraryDeployTasks(final ModelMap<Task> tasks, final ExtensionContainer ext, final NativeBinarySpec binary) {
            def deployExt = ext.getByType(DeployExtension)
            def artifacts = deployExt.artifacts

            deployExt.artifacts
                    .withType(FRCNativeArtifact)
                    .matching { FRCNativeArtifact art ->
                        art.component.equalsIgnoreCase(binary.component.name) &&
                        art.targetPlatform.equalsIgnoreCase(binary.targetPlatform.name)
                    }
                    .all { FRCNativeArtifact art ->
                        art._bin = binary
                    }
        }
    }

}
