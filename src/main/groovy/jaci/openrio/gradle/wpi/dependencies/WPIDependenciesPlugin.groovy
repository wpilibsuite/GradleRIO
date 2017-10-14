package jaci.openrio.gradle.wpi.dependencies

import jaci.openrio.gradle.GradleRIOPlugin
import jaci.openrio.gradle.wpi.WPIExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileCollectionVisitor
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.language.c.tasks.CCompile
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.DependentSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.Each
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeLibraryBinary
import org.gradle.nativeplatform.PrebuiltLibraries
import org.gradle.nativeplatform.PrebuiltLibrary
import org.gradle.nativeplatform.Repositories
import org.gradle.nativeplatform.StaticLibraryBinary
import org.gradle.nativeplatform.internal.prebuilt.AbstractPrebuiltLibraryBinary
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinarySpec

class WPIDependenciesPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class)

        project.repositories.maven { repo ->
            repo.name = "WPI"
            repo.url = "http://first.wpi.edu/FRC/roborio/maven/release"
        }

        project.repositories.maven { repo ->
            repo.name = "Jaci"
            repo.url = "http://dev.imjac.in/maven/"
        }

        apply_wpi_dependencies(project, project.extensions.wpi)
        apply_third_party_drivers(project, project.extensions.wpi)
    }

    // TODO: Move most of this to EmbeddedTools
    static class WPIDepRules extends RuleSource {
        @Model("libraries")
        void createLibrariesModel(NativeDependenciesSpec spec) { }

        @Mutate
        void addNativeLibrariesRepos(ModelMap<Task> tasks, final Repositories repos, final NativeDependenciesSpec spec) {
            PrebuiltLibraries prelibs = repos.maybeCreate('gradlerio', PrebuiltLibraries)
            spec.each { NativeLibSpec lib ->
                def libname = lib.backingNode.path.name
                // Add this to allow linking from the DSL
                prelibs.create(libname) { }
            }
        }

        @Mutate
        void addNativeLibraries(ModelMap<Task> tasks, final BinaryContainer binaries, final NativeDependenciesSpec spec, final ExtensionContainer extensions) {
            Project project = extensions.getByType(GradleRIOPlugin.ProjectWrapper).project
            binaries.withType(NativeBinarySpec).findAll { NativeBinarySpec bin ->
                bin.inputs.withType(DependentSourceSet) { lss ->
                    def addedLibs = []
                    lss.libs.forEach { lib ->
                        if (lib instanceof LinkedHashMap) {
                            def speclib = spec.get(lib["library"])
                            if (speclib != null && speclib.companions != null)
                                addedLibs += speclib.companions
                        }
                    }
                    lss.libs.addAll(addedLibs.collect { [library: it] })
                }
            }
            spec.each { NativeLibSpec lib ->
                def libname = lib.backingNode.path.name
                FileTree rootTree, sharedFiles, staticFiles
                if (lib.getMaven() != null) {
                    // Fetch from maven, add to project dependencies
                    def cfg = project.configurations.maybeCreate("native_${libname}")
                    project.dependencies.add(cfg.name, lib.getMaven())

                    rootTree = project.zipTree(cfg.dependencies.collectMany { cfg.files(it) }.first())
                } else {
                    if (lib.getFile().isDirectory()) {
                        // Assume directory including static, shared and headers
                        rootTree = project.fileTree(lib.getFile())
                    } else {
                        // Assume ZIP File including static, shared and headers
                        rootTree = project.zipTree(lib.getFile())
                    }
                }

//                headerFiles = ((ConfigurableFileTree)rootTree).from(lib.headerDirs)
                sharedFiles = rootTree.matching { pat -> pat.include(lib.sharedMatchers) }
                staticFiles = rootTree.matching { pat -> pat.include(lib.staticMatchers) }

                // We can't host a single library with multiple static/shared libs easily, so instead we'll just add it manually
                binaries.withType(NativeBinarySpec).findAll { NativeBinarySpec bin ->
                    (lib.targetPlatforms.collect { it.toLowerCase() }.contains(bin.targetPlatform.name.toLowerCase())) &&
                    (bin.inputs.withType(DependentSourceSet).collectMany { lss ->
                        lss.libs.findAll { lsslib ->
                            (lsslib instanceof LinkedHashMap && lsslib["library"] == libname)      // TODO make our own libraries
                        }
                    }.size() > 0)
                }.forEach { NativeBinarySpec bs ->
                    // Remove our temporary library
                    bs.inputs.withType(DependentSourceSet).forEach { lss -> lss.libs.removeIf { it instanceof LinkedHashMap && it["library"] == libname } }
                    bs.tasks.withType(AbstractNativeCompileTask) { task ->
                        task.doFirst() {
                            def headerDirs = lib.headerDirs.collect { new File(rootTree.asFileTrees.first().dir, it) }
                            def libdirs = (sharedFiles + staticFiles).files.collect { it.parentFile }.unique()
                            bs.cCompiler.args(*headerDirs.collectMany { ["-I", it.absolutePath] })
                            bs.cppCompiler.args(*headerDirs.collectMany { ["-I", it.absolutePath] })
                            bs.linker.args(*libdirs.collectMany { ["-L", it.absolutePath] })
                            if (lib.libraries != null)
                                bs.linker.args(*lib.libraries.collectMany { ["-l", it] })
                        }
                    }
                    println "Done"
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
