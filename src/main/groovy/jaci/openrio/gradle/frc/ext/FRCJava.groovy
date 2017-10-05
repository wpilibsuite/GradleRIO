package jaci.openrio.gradle.frc.ext

import jaci.gradle.deployers.JavaArtifact

class FRCJava extends FRCExtConfig {
    public String jarTask
    public Closure<JavaArtifact> artifact

    public List<String> jvmargs
    public List<String> arguments
    public boolean debug

    public String robotMainClass
    public List<Closure> extraManifest
    public boolean addManifest

    public boolean configureFatJar

    public FRCJava(String name) {
        super(name)
        this.jarTask = "jar"
        this.jvmargs = []
        this.arguments = []
        def debugflags = "-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=8348,server=y,suspend=y"
        this.robotCommand = {
            "/usr/local/frc/bin/netconsole-host /usr/local/frc/JRE/bin/java -Djava.library.path=/usr/local/frc/lib/ ${jvmargs.join(" ")} ${debug ? debugflags : ""} -jar <<BINARY>> ${arguments.join(" ")}"
        }

        this.robotMainClass = "Robot"
        this.extraManifest = []
        this.addManifest = true
        this.configureFatJar = true // Fat jar includes manifest + all linked libs. This is almost always true
    }

    def artifact(Closure<JavaArtifact> artifact) {
        this.artifact = artifact
    }

    def jarTask(String jarTask) {
        this.jarTask = jarTask
    }

    def argument(String argument) {
        this.arguments << argument
    }

    def jvmarg(String jvmarg) {
        this.jvmargs << jvmarg
    }

    def debug(boolean toDebug) {
        this.debug = toDebug
    }

    def manifest(boolean useManifest) {
        this.addManifest = useManifest
    }

    def manifest(final Closure config) {
        this.addManifest = true
        this.extraManifest << config
    }

    def robotClass(String mainClass) {
        this.robotMainClass = mainClass
    }
    
    def fatJar(boolean toConfigureJarFat) {
        this.configureFatJar = toConfigureJarFat
    }
}
