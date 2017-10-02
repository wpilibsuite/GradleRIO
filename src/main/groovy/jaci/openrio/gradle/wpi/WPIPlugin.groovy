package jaci.openrio.gradle.wpi

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.security.MessageDigest

class WPIPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("wpi", WPIExtension, project)

        project.repositories.maven {
            name = "WPI"
            url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven {
            name = "Jaci"
            url = "http://dev.imjac.in/maven/"
        }

        // This is needed because Gradle will not download dependencies
        // until they are referenced, so we need to do this before build
        // as otherwise deploy will not work (will try to download while
        // not connected to the internet)
        project.task("resolveNativeDeps") {
            group "GradleRIO"
            description "Resolve Dependencies from Maven"
            //project.tasks.getByName("build").dependsOn it
            doLast {
                def conf = [project.configurations.nativeLib, project.configurations.nativeZip]
                conf.each { c -> 
                    c.dependencies.findAll { it != null }.collect {
                        def libfile = c.files(it)[0]
                    }
                }
            }
        }

        project.task("wpiVersions") {
            group "GradleRIO"
            description "Print all versions of things in the wpi block"
            doLast {
                println "WPILib: ${project.wpi.wpilibVersion}"
                println "NTCore: ${project.wpi.ntcoreVersion}"
                println "OpenCV: ${project.wpi.opencvVersion}"
                println "CSCore: ${project.wpi.cscoreVersion}"
                println "CTRE: ${project.wpi.ctreVersion}"
                println "NavX: ${project.wpi.navxVersion}"
            }
        }

        project.with {
            afterEvaluate {
                build.dependsOn resolveNativeDeps
            }
        }

        project.wpi.ext.recommended = { year ->
            def md5 = MessageDigest.getInstance("MD5")
            md5.update(year.bytes)
            def cachename = md5.digest().encodeHex().toString()
            def cachefolder = new File(GradleRIO.getGlobalDirectory(), "cache/recommended")
            cachefolder.mkdirs()
            def cachefile = new File(cachefolder, cachename)

            def versions = null

            if (project.gradle.startParameter.isOffline()) {
                // Access Cache
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

            versions = new groovy.json.JsonSlurper().parseText(versions)[year]
            project.with {
                wpi {
                    wpilibVersion versions["wpilib"] ?: "+"
                    ntcoreVersion versions["ntcore"] ?: "+"
                    opencvVersion versions["opencv"] ?: "+"
                    cscoreVersion versions["cscore"] ?: "+"
                    ctreVersion versions["ctre"] ?: "+"
                    navxVersion versions["navx"] ?: "+"
                }
            }
        }

        apply_wpi_dependencies(project);
        apply_third_party_drivers(project);
    }

    void apply_wpi_dependencies(Project project) {
        project.repositories.maven {
            name = "WPI"
            url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        // Add WPILib to your project:
        // dependencies {
        //     compile wpilib()
        // }

        project.dependencies.ext.wpilibNative = {
            ["edu.wpi.first.wpilibj:athena-jni:${project.wpi.wpilibVersion}",
             "org.opencv:opencv-jni:${project.wpi.opencvVersion}:linuxathena",
             "edu.wpi.first.wpilib:athena-runtime:${project.wpi.wpilibVersion}@zip",
             "edu.wpi.cscore.java:cscore:${project.wpi.cscoreVersion}:athena-uberzip@zip"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibNative().each {
                project.dependencies.add("nativeZip", it)
            }
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:arm",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:desktop",
             "org.opencv:opencv-java:${project.wpi.opencvVersion}",
             "edu.wpi.cscore.java:cscore:${project.wpi.cscoreVersion}:arm"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:athena:${project.wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${project.wpi.ntcoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project) {

        // dependencies {
        //     compile ctre()
        //     compile navx()
        //     
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     nativeLib  fileTree(dir: 'libs', include: '**/*.so')
        // }

        project.dependencies.ext.ctreJni = {
            "thirdparty.frc.ctre:Toolsuite-Zip:${project.wpi.ctreVersion}@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${project.wpi.ctreVersion}"]
        }

        project.dependencies.ext.navx = {
            ["thirdparty.frc.kauai:Navx-Java:${project.wpi.navxVersion}"]
        }
    }
}