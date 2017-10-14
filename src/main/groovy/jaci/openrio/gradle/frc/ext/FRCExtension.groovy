package jaci.openrio.gradle.frc.ext

import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer

@CompileStatic
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

    def nativePlatform(final Closure config) {
        nativ.configure(config)
    }
}
