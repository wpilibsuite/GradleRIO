package jaci.openrio.gradle.frc.ext

import jaci.gradle.deployers.NativeArtifact

class FRCNative extends FRCExtConfig {
    public String component
    public Closure<NativeArtifact> artifact

    public List<String> arguments

    public FRCNative(String name) {
        super(name)

        this.arguments = []
        this.robotCommand = {
            "/usr/local/frc/bin/netconsole-host <<BINARY>> ${arguments.join(" ")}"
        }
    }

    def artifact(Closure<NativeArtifact> artifact) {
        this.artifact = artifact
    }

    def component(String component) {
        this.component = component
    }

    def argument(String argument) {
        this.arguments << argument
    }

    def withArguments(Closure<List<String>> args) {
        arguments.with(args)
    }

}
