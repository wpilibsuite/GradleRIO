package jaci.openrio.gradle.deploy

import jaci.gradle.deployers.JavaArtifact

class FRCJava extends FRCDeployer {
    public String jarTask
    public Closure<JavaArtifact> artifact

    public List<String> jvmargs
    public List<String> arguments
    public boolean debug

    public FRCJava(String name) {
        super(name)
        this.jarTask = "jar"
        this.jvmargs = []
        this.arguments = []
        def debugflags = "-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=8348,server=y,suspend=y"
        this.robotCommand = {
            "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs.join(" ")} ${debug ? debugflags : ""} -jar <<BINARY>> ${arguments.join(" ")}"
        }
    }

    public void artifact(Closure<JavaArtifact> artifact) {
        this.artifact = artifact
    }

    public void jarTask(String jarTask) {
        this.jarTask = jarTask
    }

    public void argument(String argument) {
        this.arguments << argument
    }

    public void jvmarg(String jvmarg) {
        this.jvmargs << jvmarg
    }

    public void debug(boolean toDebug) {
        this.debug = toDebug
    }
}
