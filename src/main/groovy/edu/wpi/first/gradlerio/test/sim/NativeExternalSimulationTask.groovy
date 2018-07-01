package edu.wpi.first.gradlerio.test.sim

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import jaci.gradle.nativedeps.DelegatedDependencySet
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.language.nativeplatform.HeaderExportingSourceSet
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.toolchain.Clang

import java.nio.file.Paths

@CompileStatic
class NativeExternalSimulationTask extends DefaultTask {
    @Internal
    List<NativeExecutableBinarySpec> binaries = []

    @TaskAction
    void create() {
        def cfgs = []
        def extensions = SimulationPlugin.getHALExtensions(project)
        for (NativeExecutableBinarySpec binary : binaries) {
            def cfg = [:]
            def installTask = (InstallExecutable)binary.tasks.install
            cfg['name'] = binary.component.name
            cfg['extensions'] = extensions
            cfg['launchfile'] = Paths.get(installTask.installDirectory.asFile.get().toString(), 'lib', installTask.executableFile.asFile.get().name).toString()
            cfg['clang'] = binary.toolChain in Clang

            def srcpaths = []
            def headerpaths = []
            def libpaths = []
            def libsrcpaths = []
            binary.inputs.withType(HeaderExportingSourceSet) { HeaderExportingSourceSet ss ->
                srcpaths += ss.source.srcDirs
                srcpaths += ss.exportedHeaders.srcDirs
            }
            binary.libs.each { NativeDependencySet ds ->
                headerpaths += ds.includeRoots
                libpaths += ds.runtimeFiles.files
                if (ds instanceof DelegatedDependencySet) {
                    libsrcpaths += (ds as DelegatedDependencySet).getSourceFiles()
                }
            }

            cfg['srcpaths'] = (srcpaths as List<File>).collect { it.absolutePath }
            cfg['headerpaths'] = (headerpaths as List<File>).collect { it.absolutePath }
            cfg['libpaths'] = (libpaths as List<File>).collect { it.absolutePath }
            cfg['libsrcpaths'] = (libsrcpaths as List<File>).collect { it.absolutePath }

            cfgs << cfg
        }

        def gbuilder = new GsonBuilder()
        gbuilder.setPrettyPrinting()
        def json = gbuilder.create().toJson(cfgs)

        println(json)

        def outfile = new File(project.buildDir, "debug/desktopinfo.json")
        outfile.parentFile.mkdirs()
        outfile.text = json
    }
}
