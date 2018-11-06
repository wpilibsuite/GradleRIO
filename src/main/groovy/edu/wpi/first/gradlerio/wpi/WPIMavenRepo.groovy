package edu.wpi.first.gradlerio.wpi

import groovy.transform.CompileStatic
import org.gradle.api.Named

@CompileStatic
class WPIMavenRepo implements Named {
    String release = null
    String development = null
    int priority = PRIORITY_REPO

    String name;

    static final int PRIORITY_REPO = 100
    static final int PRIORITY_REPO_INUSE = 50

    static final int PRIORITY_OFFICIAL = 150
    static final int PRIORITY_MIRROR = 200
    static final int PRIORITY_MIRROR_INUSE = 120

    static final int PRIORITY_VENDOR = 175

    WPIMavenRepo(String name) {
        this.name = name
    }
}
