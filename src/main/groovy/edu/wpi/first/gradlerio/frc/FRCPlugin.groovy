package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.frc.riolog.RiologPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.*
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.nativedeps.DelegatedDependencySet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.nativeplatform.DependentSourceSet
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.BinaryTasks

import java.util.function.Function

@CompileStatic
class FRCPlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.pluginManager.apply(EmbeddedTools)
        project.pluginManager.apply(RiologPlugin)

        def debugInfoLazy = project.tasks.register("writeDebugInfo", DebugInfoTask)

        project.tasks.withType(ArtifactDeployTask).configureEach { ArtifactDeployTask t ->
            t.dependsOn(debugInfoLazy)
        }

        // Helper Extensions
        project.extensions.add("getTeamOrDefault", { Integer teamDefault ->
            if (project.hasProperty("teamNumber"))
                return Integer.parseInt(project.property("teamNumber") as String)

            def number = getTeamNumberFromJSON()
            if (number < 0)
                return teamDefault
            return number
        } as Closure<Integer>);

        project.extensions.add("getTeamNumber", {
            if (project.hasProperty("teamNumber"))
                return Integer.parseInt(project.property("teamNumber") as String)
            def number = getTeamNumberFromJSON()
            if (number < 0)
                throw new TeamNumberNotFoundException()
            return number
        } as Closure<Integer>);

        project.extensions.add("getDebugOrDefault", { Boolean debugDefault ->
            if (project.hasProperty("debugMode"))
                return true
            return debugDefault
        } as Closure<Boolean>);


        project.afterEvaluate {
            addNativeLibraryArtifacts(project)
            addJreArtifact(project)
            addCommandArtifacts(project)
        }
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
                pat.include('*.so*', 'lib/*.so', 'java/lib/*.so', 'linux/athena/shared/*.so', '**/libopencv*.so.*')
            }
        }
    }

    private int getTeamNumberFromJSON() {
        def jsonFile = project.rootProject.file(".wpilib/wpilib_preferences.json")
        if (jsonFile.exists()) {
            def parsedJson = new groovy.json.JsonSlurper().parseText(jsonFile.text)
            def teamNumber = parsedJson['teamNumber']
            if (teamNumber != null)
                return teamNumber as Integer
        }
        return -1;
    }

    static class FRCRules extends RuleSource {
        @BinaryTasks
        void createNativeLibraryDeployTasks(final ModelMap<Task> tasks, final ExtensionContainer ext, final NativeBinarySpec binary) {
            def deployExt = ext.getByType(DeployExtension)
            def artifacts = deployExt.artifacts

            // We can't use binary.libs because that forces a reenumeration of all libraries,
            // which breaks multiproject builds. Instead, we have to resolve manually
            binary.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                dss.libs.each { Object lib ->
                    if (lib instanceof DelegatedDependencySet) {
                        DelegatedDependencySet set = (DelegatedDependencySet)lib
                        if (artifacts.findByName(set.getName()) == null) {
                            artifacts.nativeLibraryArtifact(set.getName()) { NativeLibraryArtifact nla ->
                                FRCPlugin.allFrcTargets(deployExt, nla)
                                nla.directory = '/usr/local/frc/lib'
                                nla.postdeploy << { DeployContext ctx -> ctx.execute('ldconfig') }
                                nla.library = set.name
                                nla.targetPlatform = 'roborio'
                            }
                        }
                    }
                }
            }

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
