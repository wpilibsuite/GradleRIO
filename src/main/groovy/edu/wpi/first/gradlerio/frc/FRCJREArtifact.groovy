package edu.wpi.first.gradlerio.frc

import de.undercouch.gradle.tasks.download.DownloadAction
import edu.wpi.first.gradlerio.GradleRIOPlugin
import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.FileArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

import java.util.function.Function

@CompileStatic
class FRCJREArtifact extends FileArtifact {

    private final String zuluJreUrl = "https://github.com/wpilibsuite/zulu-jre-ipk/releases/download/v2018.1/zulu-jre_1.8.0-131_cortexa9-vfpv3.ipk"
    private File dest

    Function<DeployContext, Boolean> buildRequiresJre = (Function<DeployContext, Boolean>){ true }

    FRCJREArtifact(String name, Project project) {
        super(name, project)
        dest = new File(GradleRIOPlugin.globalDirectory, 'jre/zulu/JreZulu_18u131_kickoff.ipk')
        dest.parentFile.mkdirs()

        // You absolutely should not download during configuration, but since
        // there is no internet connection during a deploy, this JRE is an exception.
        // The JRE will be added as a maven dependency for 2019 beta.
        if (!project.gradle.startParameter.isOffline()) {
            def da = new DownloadAction(project)
            da.with { DownloadAction d ->
                d.src zuluJreUrl
                d.dest dest
                d.overwrite false
            }
            da.execute()
        }

        onlyIf = { DeployContext ctx ->
            dest.exists() && buildRequiresJre.apply(ctx) && jreMissing(ctx)
        }

        predeploy << { DeployContext ctx ->
            ctx.logger.log('Deploying RoboRIO Zulu JRE (this will take a while)...')
        }

        file.set(dest)
        directory = '/tmp'
        filename = 'zulujre.ipk'

        postdeploy << { DeployContext ctx ->
            ctx.logger.log('Installing JRE...')
            ctx.execute('opkg remove zulu-jre*; opkg install /tmp/zulujre.ipk; rm /tmp/zulujre.ipk')
            ctx.logger.log('JRE Deployed!')
        }
    }

    boolean jreMissing(DeployContext ctx) {
        return ctx.execute('if [[ -f "/usr/local/frc/JRE/bin/java" ]]; then echo OK; else echo MISSING; fi')
    }
}
