package jaci.openrio.gradle.wpi

import groovy.json.JsonSlurper
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.security.MessageDigest

class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        WPIExtension wpiExtension = project.extensions.create("wpi", WPIExtension, project)

        project.pluginManager.apply(WPITools)
        new WPIDependencies().apply(project)

        project.task("wpi") { Task task ->
            task.group = "GradleRIO"
            task.description = "Print all versions of the wpi block"
            task.doLast {
                wpiExtension.versions().forEach { key, tup ->
                    println "${tup.first()}: ${tup[1]} (${key})"
                }
            }
        }

        wpiExtension.ext.recommended = { String year ->
            def md5 = MessageDigest.getInstance("MD5")
            md5.update(year.bytes)
            def cachename = md5.digest().encodeHex().toString()
            def cachefolder = new File(GradleRIOPlugin.getGlobalDirectory(), "cache/recommended")
            cachefolder.mkdirs()
            def cachefile = new File(cachefolder, cachename)

            def versions = null

            if (project.gradle.startParameter.isOffline()) {
                println "Using offline recommended version cache..."
                versions = cachefile.text
            } else {
                try {
                    versions = "http://openrio.imjac.in/gradlerio/recommended".toURL().text
                    cachefile.text = versions
                } catch (all) {
                    println "Using offline recommended version cache..."
                    versions = cachefile.text
                }
            }

            versions = new JsonSlurper().parseText(versions)[year]
            wpiExtension.versions().forEach { property, tuple ->
                wpiExtension.setProperty(property, versions[tuple.last()] ?: wpiExtension.getProperty(property))
            }
        }
    }
}