package jaci.openrio.gradle.deploy

import org.gradle.api.NamedDomainObjectContainer

public class FRCExtension {
    NamedDomainObjectContainer<RoboRIO> roborio
    NamedDomainObjectContainer<FRCJava> java
    NamedDomainObjectContainer<FRCNative> nativ

    FRCExtension(NamedDomainObjectContainer<RoboRIO> roborio,
                 NamedDomainObjectContainer<FRCJava> java,
                 NamedDomainObjectContainer<FRCNative> nativ) {
        this.roborio = roborio
        this.java = java
        this.nativ = nativ
    }

    def roborio(final Closure config) {
        roborio.configure(config)
    }

    def javaDeploy(final Closure config) {
        java.configure(config)
    }

    def nativeDeploy(final Closure config) {
        nativ.configure(config)
    }
}
