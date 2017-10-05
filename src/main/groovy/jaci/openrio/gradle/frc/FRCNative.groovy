package jaci.openrio.gradle.frc

import jaci.gradle.deployers.NativeArtifact

class FRCNative extends FRCDeployer {
    public Closure<NativeArtifact> artifact

    public FRCNative(String name) {
        super(name)
    }

    def artifact(Closure<NativeArtifact> artifact) {
        this.artifact = artifact
    }
}
