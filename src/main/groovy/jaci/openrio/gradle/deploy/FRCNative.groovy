package jaci.openrio.gradle.deploy

import jaci.gradle.deployers.NativeArtifact

class FRCNative extends FRCDeployer {
    public Closure<NativeArtifact> artifact

    public FRCNative(String name) {
        super(name)
    }

    public void artifact(Closure<NativeArtifact> artifact) {
        this.artifact = artifact
    }
}
