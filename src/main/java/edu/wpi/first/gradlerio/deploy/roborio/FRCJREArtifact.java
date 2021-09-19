package edu.wpi.first.gradlerio.deploy.roborio;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.deployutils.deploy.artifact.MavenArtifact;
import edu.wpi.first.deployutils.deploy.context.DeployContext;
import edu.wpi.first.deployutils.deploy.target.RemoteTarget;
import edu.wpi.first.gradlerio.deploy.DeployStage;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class FRCJREArtifact extends MavenArtifact {
    private final String configName;

    public String getConfigName() {
        return configName;
    }

    @Inject
    public FRCJREArtifact(String name, RemoteTarget target) {
        super(name, target);
        String configName = name + "frcjre";
        this.configName = configName;
        Project project = target.getProject();
        getConfiguration().set(project.getConfigurations().create(configName));
        getDependency().set(project.getDependencies().add(configName, project.getExtensions().getByType(WPIExtension.class).getJreArtifactLocation()));

        setOnlyIf(ctx -> {
            return jreMissing(ctx) || project.hasProperty("force-redeploy-jre");
        });

        getDirectory().set("/tmp");
        getFilename().set("frcjre.ipk");

        getPostdeploy().add(ctx -> {
            ctx.getLogger().log("Installing JRE...");
            ctx.execute("opkg remove frc2021-openjdk*; opkg install /tmp/frcjre.ipk; rm /tmp/frcjre.ipk");
            ctx.getLogger().log("JRE Deployed!");
        });

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    private boolean jreMissing(DeployContext ctx) {
        return ctx.execute("if [[ -f \"/usr/local/frc/JRE/bin/java\" ]]; then echo OK; else echo MISSING; fi").getResult().contains("MISSING");
    }


}
