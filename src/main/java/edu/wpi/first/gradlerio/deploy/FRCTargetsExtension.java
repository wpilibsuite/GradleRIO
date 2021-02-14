package edu.wpi.first.gradlerio.deploy;

import javax.inject.Inject;

import org.gradle.api.Action;

import edu.wpi.first.embeddedtools.deploy.target.TargetsExtension;

public class FRCTargetsExtension {
    private final TargetsExtension targets;

    @Inject
    public FRCTargetsExtension(TargetsExtension targets) {
        this.targets = targets;
    }

    public RoboRIO roboRIO(String name, final Action<RoboRIO> config) {
        return targets.target(name, RoboRIO.class, config);
    }
}
