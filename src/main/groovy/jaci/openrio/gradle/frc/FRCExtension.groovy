package jaci.openrio.gradle.frc

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

    def java(final Closure config) {
        java.configure(config)
    }

    def nativeplatform(final Closure config) {
        nativ.configure(config)
    }
}
