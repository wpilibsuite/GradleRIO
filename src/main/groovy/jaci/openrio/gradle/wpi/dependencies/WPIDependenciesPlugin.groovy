package jaci.openrio.gradle.wpi.dependencies

import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.PrebuiltLibraries
import org.gradle.nativeplatform.Repositories

class WPIDependenciesPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.repositories.maven { repo ->
            repo.name = "WPI"
            repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven { repo ->
            repo.name = "Jaci"
            repo.url = "http://dev.imjac.in/maven/"
        }

        println("Hello World")

        apply_wpi_dependencies(project, project.extensions.wpi)
        apply_third_party_drivers(project, project.extensions.wpi)
    }

    static class WPIDepRules extends RuleSource {
        @Model("nativeLibs")
        void createLibrariesModel(NativeDependenciesSpec spec) {
            println("B")
        }

        @Mutate
        void addNativeLibraries(Repositories repos, @Path("nativeLibs") NativeDependenciesSpec spec, final ExtensionContainer extensions) {
            println("A")
            Project project = extensions.getByName('projectWrapper').project

            PrebuiltLibraries prelibs = repos.maybeCreate('gradlerio', PrebuiltLibraries)
            spec.each { NativeLibSpec lib ->
                def libname = lib.backingNode.path.name
                println(libname)
                if (lib.getMaven() != null) {
                    // Fetch from maven, add to project dependencies
                    def cfg = project.configurations.maybeCreate("native_${libname}")
                    project.dependencies.add(cfg.name, lib.getMaven())

                    def destdir = new File(project.buildDir, "native_extract/${libname}")
                    destdir.mkdirs()
                    // Load dependency zip file
                    project.copy { c ->
                        c.from project.zipTree(cfg.dependencies.collectMany { cfg.files(it) }.first())
                        c.into destdir
                    }
                } else {
                    // Load the files directly
                }
            }
        }
    }

    void apply_wpi_dependencies(Project project, WPIExtension wpi) {

        // Add WPILib to your project:
        // Java:
        // dependencies {
        //     compile wpilib()
        // }

        // C++ Libraries will need special consideration since they aren't automatically fetched, unzipped and linked in
        // a component spec. We'll have to put in our own facets for this.
//        project.dependencies.ext.wpilibNative = {
//            ["edu.wpi.first.wpilibc:athena:${wpi.wpilibVersion}",
//            "edu.wpi.first.wpilib:hal:${wpi.wpilibVersion}",
//            "edu.wpi.first.wpilib:wpiutil:${wpi.wpiutilVersion}:arm@zip",
//            "edu.wpi.first.wpilib.networktables.cpp:NetworkTables:${wpi.ntcoreVersion}:arm@zip",
//            "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:athena-uberzip@zip"]
//        }

        project.dependencies.ext.wpilibJni = {
            ["edu.wpi.first.wpilibj:athena-jni:${wpi.wpilibVersion}",
             "org.opencv:opencv-jni:${wpi.opencvVersion}:${wpi.opencvVersion == "3.1.0" ? "linux-arm" : "linuxathena"}",
             "edu.wpi.first.wpilib:athena-runtime:${wpi.wpilibVersion}@zip",
             "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:athena-uberzip@zip"]
        }

        project.dependencies.ext.wpilib = {
            project.dependencies.ext.wpilibJni().each {
                project.dependencies.add("nativeZip", it)
            }
            ["edu.wpi.first.wpilibj:athena:${wpi.wpilibVersion}",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:arm",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:desktop",
             "org.opencv:opencv-java:${wpi.opencvVersion}",
             "edu.wpi.cscore.java:cscore:${wpi.cscoreVersion}:arm"]
        }

        project.dependencies.ext.wpilibSource = {
            ["edu.wpi.first.wpilibj:athena:${wpi.wpilibVersion}:sources",
             "edu.wpi.first.wpilib.networktables.java:NetworkTables:${wpi.ntcoreVersion}:sources"]
        }
    }

    void apply_third_party_drivers(Project project, WPIExtension wpi) {

        // Java:
        // dependencies {
        //     compile ctre()
        //     compile navx()
        //
        //     // Use this to include a device library we don't provide, from your file system.
        //     compile fileTree(dir: 'libs', include: '**/*.jar')
        //     nativeLib  fileTree(dir: 'libs', include: '**/*.so')
        // }

//        project.dependencies.ext.ctreNative = {
//            "thirdparty.frc.ctre:Toolsuite-Zip:${wpi.ctreVersion}@zip"
//        }

        project.dependencies.ext.ctreJni = {
            "thirdparty.frc.ctre:Toolsuite-Zip:${wpi.ctreVersion}@zip"
        }

        project.dependencies.ext.ctre = {
            project.dependencies.add("nativeZip", project.dependencies.ext.ctreJni())
            ["thirdparty.frc.ctre:Toolsuite-Java:${wpi.ctreVersion}"]
        }

//        project.dependencies.ext.navxNative = {
//            "thirdparty.frc.kauai:Navx-Zip:${wpi.navxVersion}@zip"
//        }

        project.dependencies.ext.navx = {
            ["thirdparty.frc.kauai:Navx-Java:${wpi.navxVersion}"]
        }
    }
}
