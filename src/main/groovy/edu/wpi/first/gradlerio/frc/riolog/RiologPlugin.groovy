package edu.wpi.first.gradlerio.frc.riolog

import edu.wpi.first.gradlerio.frc.RIOLogTask
import edu.wpi.first.gradlerio.frc.RoboRIO
import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

@CompileStatic
class RiologPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(EmbeddedTools)
        project.extensions.getByType(DeployExtension).targets.all { RemoteTarget target ->
            if (target instanceof RoboRIO) {
                def rioLogTask = project.tasks.register("riolog${target.name.capitalize()}".toString(), RIOLogTask, { RIOLogTask task ->
                    task.group = "GradleRIO"
                    task.description = "Run a console displaying output from the RoboRIO (${target.name})"
                    task.dependsOn(project.tasks.withType(TargetDiscoveryTask).matching { TargetDiscoveryTask t -> t.target == target })
                } as Action<RIOLogTask>)

                // Guard for when the root project already has riolog
                try {
                    project.rootProject.tasks.named('riolog')
                } catch (UnknownTaskException ignored) {
                    project.rootProject.tasks.register('riolog', RIOLogTask, { RIOLogTask task ->
                        task.group = "GradleRIO"
                        task.description = "Run a console displaying output from the default RoboRIO (${target.name})"
                        task.dependsOn("riolog${target.name.capitalize()}")
                    } as Action<RIOLogTask>)
                }
            }
        }
    }
}
