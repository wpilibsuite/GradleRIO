package edu.wpi.first.gradlerio.frc

import de.undercouch.gradle.tasks.download.DownloadAction
import edu.wpi.first.gradlerio.GradleRIOPlugin
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.*
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import jaci.gradle.nativedeps.DelegatedDependencySet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.platform.base.BinaryTasks

@CompileStatic
class FRCPlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.pluginManager.apply(EmbeddedTools)

        project.extensions.getByType(DeployExtension).targets.all { RemoteTarget target ->
            if (target instanceof RoboRIO) {
                project.tasks.register("riolog${target.name.capitalize()}", RIOLogTask) { RIOLogTask task ->
                    task.group = "GradleRIO"
                    task.description = "Run a console displaying output from the RoboRIO (${target.name})"
                    project.tasks.withType(TargetDiscoveryTask).matching { TargetDiscoveryTask t -> t.target == target }.all { TargetDiscoveryTask discover_task ->
                        task.dependsOn(discover_task)
                    }
                }

                // TODO: Master RIOLog with Workers?
                if (project.tasks.findByName('riolog') == null) {
                    project.tasks.register('riolog', RIOLogTask) { RIOLogTask task ->
                        task.group = "GradleRIO"
                        task.description = "Run a console displaying output from the default RoboRIO (${target.name})"
                        task.dependsOn("riolog${target.name.capitalize()}")
                    }
                }
            }
        }

        project.tasks.register("writeDebugInfo", DebugInfoTask) { DebugInfoTask task ->
            project.tasks.withType(ArtifactDeployTask).all { Task t -> t.dependsOn(task) }
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

    public static void allRoborioTargets(DeployExtension ext, Artifact artifact) {
        ext.targets.withType(RoboRIO).all { RoboRIO r ->
            artifact.targets << r.name
        }
    }

    void addCommandArtifacts(Project project) {
        // As far as I can tell, the order of this doesn't matter
        // It only comments out some stuff for the LabVIEW runtime that apparently isn't needed, and dramatically
        // reduces memory usage.
        // See https://github.com/wpilibsuite/EclipsePlugins/pull/154
        deployExtension(project).artifacts.commandArtifact('roborioCommands') { CommandArtifact artifact ->
            allRoborioTargets(deployExtension(project), artifact)
            artifact.command = "sed -i -e 's/^StartupDLLs/;StartupDLLs/' /etc/natinst/share/ni-rt.ini"
        }
    }

    void addJreArtifact(Project project) {
        // Download Zulu JRE (do this during config time in case we're not connected to both the robot and internet)
        def zuluJreUrl = "https://github.com/wpilibsuite/zulu-jre-ipk/releases/download/v2018.1/zulu-jre_1.8.0-131_cortexa9-vfpv3.ipk"
        def dest = new File(GradleRIOPlugin.globalDirectory, 'jre/zulu/JreZulu_18u131_kickoff.ipk')
        dest.parentFile.mkdirs()

        if (!project.gradle.startParameter.isOffline()) {
            def da = new DownloadAction(project)
            da.with { DownloadAction d ->
                d.src zuluJreUrl
                d.dest dest
                d.overwrite false
            }
            da.execute()
        }

        if (!dest.exists()) {
            println "Cannot find RoboRIO JRE File! Make sure your first build happens while connected to the internet!"
        } else {
            deployExtension(project).artifacts.fileArtifact('jre') { FileArtifact artifact ->
                allRoborioTargets(deployExtension(project), artifact)
                artifact.onlyIf = { DeployContext ctx ->
                    dest.exists() && (
                            (
                                    deployExtension(project).artifacts.withType(JavaArtifact).size() > 0 &&
                                    ctx.execute('if [[ -f "/usr/local/frc/JRE/bin/java" ]]; then echo OK; else echo MISSING; fi').result.toString().trim() != 'OK'
                            ) || project.hasProperty("deploy-force-jre")
                    )
                }
                artifact.predeploy << { DeployContext ctx -> ctx.logger.log("Deploying RoboRIO Zulu JRE....") }
                artifact.file = dest
                artifact.directory = '/tmp'
                artifact.filename = 'zulujre.ipk'
                artifact.postdeploy << { DeployContext ctx ->
                    ctx.execute('opkg remove zulu-jre*; opkg install /tmp/zulujre.ipk; rm /tmp/zulujre.ipk')
                }
                artifact.postdeploy << { DeployContext ctx -> ctx.logger.log("JRE Deployed!") }
            }
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
                artifact.setFiles(artifact.getFiles().get() + project.files(nativeLibs.files(dep).toArray()))
            }

            artifact.onlyIf = {
                artifact.files.isPresent() && !artifact.files.get().empty
            }
        }

        deployExtension(project).artifacts.fileCollectionArtifact('nativeZips') { FileCollectionArtifact artifact ->
            allRoborioTargets(deployExtension(project), artifact)
            artifact.files = project.files()
            artifact.directory = '/usr/local/frc/lib'
            artifact.postdeploy << { DeployContext ctx -> ctx.execute("ldconfig") }

            nativeZips.dependencies.matching { Dependency dep -> dep != null && nativeZips.files(dep).size() > 0 }.all { Dependency dep ->
                def ziptree = project.zipTree(nativeZips.files(dep).first())
                ["*.so*", "lib/*.so", "java/lib/*.so", "linux/athena/shared/*.so", "**/libopencv*.so.*"].collect { String pattern ->
                    artifact.setFiles(artifact.getFiles().get() + ziptree.matching { PatternFilterable pat -> pat.include(pattern) })
                }
            }

            artifact.onlyIf = {
                artifact.files.isPresent() && !artifact.files.get().empty
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
            binary.libs.each { NativeDependencySet set ->
                if (set instanceof DelegatedDependencySet) {
                    def delegSet = set as DelegatedDependencySet
                    artifacts.nativeLibraryArtifact(delegSet.getName()) { NativeLibraryArtifact nla ->
                        FRCPlugin.allRoborioTargets(deployExt, nla)
                        nla.directory = '/usr/local/frc/lib'
                        nla.postdeploy << { DeployContext ctx -> ctx.execute('ldconfig') }
                        nla.library = delegSet.name
                        nla.targetPlatform = 'roborio'
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
