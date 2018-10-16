package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.MavenArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

import java.util.function.Function

@CompileStatic
class FRCJREArtifact extends MavenArtifact {
    Function<DeployContext, Boolean> buildRequiresJre = (Function<DeployContext, Boolean>){ true }

    FRCJREArtifact(String name, Project project) {
        super(name, project)
        configuration = project.configurations.create(name + 'frcjre')
        dependency = project.dependencies.add(name + 'frcjre', project.extensions.getByType(WPIExtension).jreArtifactLocation)

        onlyIf = { DeployContext ctx ->
            buildRequiresJre.apply(ctx) && jreMissing(ctx)
        }

        predeploy << { DeployContext ctx ->
            ctx.logger.log('Deploying RoboRIO JRE (this will take a while)...')
        }

        directory = '/tmp'

        postdeploy << { DeployContext ctx ->
            ctx.logger.log('Installing JRE...')
            ctx.execute('opkg remove frc2019-openjdk*; opkg install /tmp/roborio-2019-11.0.0u28-1.ipk; rm /tmp/roborio-2019-11.0.0u28-1.ipk')
            ctx.logger.log('JRE Deployed!')
        }
    }

    boolean jreMissing(DeployContext ctx) {
        return ctx.execute('if [[ -f "/usr/local/frc/JRE/bin/java" ]]; then echo OK; else echo MISSING; fi').result.contains("MISSING")
    }
}
