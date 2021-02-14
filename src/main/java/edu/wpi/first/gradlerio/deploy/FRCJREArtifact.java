package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Project;

import edu.wpi.first.embeddedtools.deploy.artifact.MavenArtifact;
import edu.wpi.first.embeddedtools.deploy.context.DeployContext;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

public class FRCJREArtifact extends MavenArtifact {
    private final String configName;

    public String getConfigName() {
        return configName;
    }

    @Inject
    public FRCJREArtifact(String name, Project project) {
        super(name, project);
        String configName = name + "frcjre";
        this.configName = configName;
        setConfiguration(project.getConfigurations().create(configName));
        setDependency(project.getDependencies().add(configName, project.getExtensions().getByType(WPIExtension.class).getJreArtifactLocation()));

        setOnlyIf(ctx -> {
            return jreMissing(ctx) || project.hasProperty("force-redeploy-jre");
        });

        getDirectory().set("/tmp");
        setFilename("frcjre.ipk");

        getPostdeploy().add(ctx -> {
            ctx.getLogger().log("Installing JRE...");
            ctx.execute("opkg remove frc2020-openjdk*; opkg install /tmp/frcjre.ipk; rm /tmp/frcjre.ipk");
            ctx.getLogger().log("JRE Deployed!");
        });

        getExtensionContainer().add(DeployStage.class, "stage", DeployStage.FileDeploy);
    }

    private boolean jreMissing(DeployContext ctx) {
        return ctx.execute("if [[ -f \"/usr/local/frc/JRE/bin/java\" ]]; then echo OK; else echo MISSING; fi").getResult().contains("MISSING");
    }


}
