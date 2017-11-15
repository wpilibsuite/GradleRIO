package jaci.openrio.gradle.telemetry.providers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.ArtifactBase
import jaci.gradle.deploy.target.RemoteTarget
import jaci.openrio.gradle.frc.FRCJavaArtifact
import jaci.openrio.gradle.frc.FRCNativeArtifact
import jaci.openrio.gradle.frc.RoboRIO
import jaci.openrio.gradle.telemetry.TelemetryProvider
import org.gradle.api.Project

/**
 * TelemetryProvider for the Deploy extension (from EmbeddedTools).
 * NOTE: This reports the following ONLY (addresses / users / passwords are NOT reported):
 *      - Targets
 *          - Name
 *          - Team # (if RoboRIO)
 *          - Type (RoboRIO, RemoteTarget, etc)
 *
 *      - Artifacts (only FRC Java or FRC Native artifacts are reported)
 *          - Name
 *          - Type (FRCJavaArtifact or FRCNativeArtifact)
 */
@CompileStatic
class DeployProvider implements TelemetryProvider {

    @Override
    JsonElement telemetry(Project project) {
        def obj = new JsonObject()
        def targetObj = new JsonObject()
        def artifactObj = new JsonObject()

        def deploy = project.extensions.getByType(DeployExtension)
        deploy.targets.each { RemoteTarget target ->
            def tobj = new JsonObject()
            tobj.addProperty('type', target.class.name)
            if (target instanceof RoboRIO)
                tobj.addProperty('team', (target as RoboRIO).team)
            targetObj.add(target.name, tobj)
        }

        deploy.artifacts.each { ArtifactBase artifact ->
            def aobj = new JsonObject()
            aobj.addProperty('type', artifact.class.name)

            if (artifact instanceof FRCJavaArtifact || artifact instanceof FRCNativeArtifact)
                artifactObj.add(artifact.name, aobj)
        }

        obj.add('targets', targetObj)
        obj.add('artifacts', artifactObj)
        return obj
    }

}
