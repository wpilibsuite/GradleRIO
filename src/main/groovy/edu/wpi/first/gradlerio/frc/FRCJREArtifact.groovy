package edu.wpi.first.gradlerio.frc

import edu.wpi.first.gradlerio.wpi.WPIExtension
import groovy.transform.CompileStatic
import javax.inject.Inject
import edu.wpi.first.embeddedtools.deploy.artifact.MavenArtifact
import edu.wpi.first.embeddedtools.deploy.context.DeployContext
import org.gradle.api.Project
import edu.wpi.first.embeddedtools.ActionWrapper

import java.util.function.Function

@CompileStatic
class FRCJREArtifact extends MavenArtifact {
    Function<DeployContext, Boolean> buildRequiresJre = (Function<DeployContext, Boolean>){ true }

    @Inject
    FRCJREArtifact(String name, Project project) {
        super(name, project)
        configuration = project.configurations.create(configuration())
        dependency = project.dependencies.add(configuration(), project.extensions.getByType(WPIExtension).jreArtifactLocation)

        onlyIf = { DeployContext ctx ->
            (buildRequiresJre.apply(ctx) && jreMissing(ctx)) || project.hasProperty("force-redeploy-jre")
        }

        predeploy << new ActionWrapper({ DeployContext ctx ->
            ctx.logger.log('Deploying RoboRIO JRE (this will take a while)...')
        })

        directory = '/tmp'
        filename = 'frcjre.ipk'

        postdeploy << new ActionWrapper({ DeployContext ctx ->
            ctx.logger.log('Installing JRE...')
            ctx.execute('opkg remove frc2020-openjdk*; opkg install /tmp/frcjre.ipk; rm /tmp/frcjre.ipk')
            ctx.logger.log('JRE Deployed!')
        })
    }

    String configuration() {
        return name + 'frcjre'
    }

    boolean jreMissing(DeployContext ctx) {
        return ctx.execute('if [[ -f "/usr/local/frc/JRE/bin/java" ]]; then echo OK; else echo MISSING; fi').result.contains("MISSING")
    }
}
