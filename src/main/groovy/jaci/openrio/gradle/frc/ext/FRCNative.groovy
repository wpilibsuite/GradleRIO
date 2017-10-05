package jaci.openrio.gradle.frc.ext

import jaci.gradle.deployers.NativeArtifact

class FRCNative extends FRCExtConfig {
    public Closure<NativeArtifact> artifact

    public FRCNative(String name) {
        super(name)
    }

    def artifact(Closure<NativeArtifact> artifact) {
        this.artifact = artifact
    }
}
