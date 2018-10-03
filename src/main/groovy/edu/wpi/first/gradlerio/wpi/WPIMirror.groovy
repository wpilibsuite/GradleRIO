package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Named

@CompileStatic
class WPIMirror implements Named {
    String release = null
    String development = null
    int priority = 100

    String name;

    WPIMirror(String name) {
        this.name = name
    }
}
