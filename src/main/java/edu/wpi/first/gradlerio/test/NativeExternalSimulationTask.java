package edu.wpi.first.gradlerio.test;

import com.google.gson.GsonBuilder;
import groovy.transform.CompileStatic;
import edu.wpi.first.embeddedtools.nativedeps.DelegatedDependencySet;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.nativeplatform.NativeDependencySet;
import org.gradle.nativeplatform.NativeExecutableBinarySpec;
import org.gradle.nativeplatform.tasks.InstallExecutable;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;
import org.gradle.nativeplatform.toolchain.Clang;
import edu.wpi.first.gradlerio.wpi.simulation.SimulationExtension;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NativeExternalSimulationTask extends ExternalSimulationTask {
    private List<NativeExecutableBinarySpec> exeBinaries = new ArrayList<>();
    private List<NativeTestSuiteBinarySpec> testBinaries = new ArrayList<>();
    private final RegularFileProperty outfile;

    @Internal
    public List<NativeExecutableBinarySpec> getExeBinaries() {
        return exeBinaries;
    }

    @Internal
    public List<NativeTestSuiteBinarySpec> getTestBinaries() {
        return testBinaries;
    }

    @OutputFile
    public RegularFileProperty getOutfile() {
        return outfile;
    }

    @Inject
    public NativeExternalSimulationTask(ObjectFactory objects) {
        outfile = objects.fileProperty();
        File file = new File(getProject().getRootProject().getBuildDir(), ExternalSimulationMergeTask.CONTAINER_FOLDER + "/" + getProject().getName() + "_native.json");
        outfile.set(file);
    }
    //File outfile =

    @TaskAction
    public void create() {
        // def cfgs = []
        // SimulationExtension simExtension = project.extensions.getByType(SimulationExtension)
        // def extensions = TestPlugin.getHALExtensions(project)
        // for (NativeExecutableBinarySpec binary : exeBinaries) {
        //     def cfg = [:]
        //     def installTask = (InstallExecutable)binary.tasks.install
        //     cfg['name'] = "${binary.component.name} (in project ${project.name})".toString()
        //     cfg['extensions'] = extensions
        //     cfg['launchfile'] = Paths.get(installTask.installDirectory.asFile.get().toString(), 'lib', installTask.executableFile.asFile.get().name).toString()
        //     cfg['clang'] = binary.toolChain in Clang
        //     cfg['env'] = simExtension.environment

        //     def srcpaths = []
        //     def headerpaths = []
        //     def libpaths = []
        //     def libsrcpaths = []
        //     def debugpaths = []
        //     binary.inputs.withType(HeaderExportingSourceSet) { HeaderExportingSourceSet ss ->
        //         srcpaths += ss.source.srcDirs
        //         srcpaths += ss.exportedHeaders.srcDirs
        //     }
        //     binary.libs.each { NativeDependencySet ds ->
        //         headerpaths += ds.includeRoots
        //         libpaths += ds.runtimeFiles.files
        //         if (ds instanceof DelegatedDependencySet) {
        //             libsrcpaths += (ds as DelegatedDependencySet).getSourceFiles()
        //             debugpaths += (ds as DelegatedDependencySet).getDebugFiles()
        //         }
        //     }

        //     cfg['srcpaths'] = (srcpaths as List<File>).collect { it.absolutePath }
        //     cfg['headerpaths'] = (headerpaths as List<File>).collect { it.absolutePath }
        //     cfg['libpaths'] = (libpaths as List<File>).collect { it.absolutePath }
        //     cfg['debugpaths'] = (debugpaths as List<File>).collect { it.absolutePath }
        //     cfg['libsrcpaths'] = (libsrcpaths as List<File>).collect { it.absolutePath }

        //     cfgs << cfg
        // }

        // for (NativeTestSuiteBinarySpec binary : testBinaries) {
        //     def cfg = [:]
        //     def installTask = (InstallExecutable)binary.tasks.install
        //     cfg['name'] = "${binary.component.name} (in project ${project.name}, test suite)".toString()
        //     cfg['extensions'] = extensions
        //     cfg['launchfile'] = Paths.get(installTask.installDirectory.asFile.get().toString(), 'lib', installTask.executableFile.asFile.get().name).toString()
        //     cfg['clang'] = binary.toolChain in Clang
        //     cfg['env'] = simExtension.environment

        //     def srcpaths = []
        //     def headerpaths = []
        //     def libpaths = []
        //     def libsrcpaths = []
        //     def debugpaths = []
        //     binary.inputs.withType(HeaderExportingSourceSet) { HeaderExportingSourceSet ss ->
        //         srcpaths += ss.source.srcDirs
        //         srcpaths += ss.exportedHeaders.srcDirs
        //     }
        //     binary.libs.each { NativeDependencySet ds ->
        //         headerpaths += ds.includeRoots
        //         libpaths += ds.runtimeFiles.files
        //         if (ds instanceof DelegatedDependencySet) {
        //             libsrcpaths += (ds as DelegatedDependencySet).getSourceFiles()
        //             debugpaths += (ds as DelegatedDependencySet).getDebugFiles()
        //         }
        //     }

        //     cfg['srcpaths'] = (srcpaths as List<File>).collect { it.absolutePath }
        //     cfg['headerpaths'] = (headerpaths as List<File>).collect { it.absolutePath }
        //     cfg['libpaths'] = (libpaths as List<File>).collect { it.absolutePath }
        //     cfg['debugpaths'] = (debugpaths as List<File>).collect { it.absolutePath }
        //     cfg['libsrcpaths'] = (libsrcpaths as List<File>).collect { it.absolutePath }

        //     cfgs << cfg
        // }

        // def gbuilder = new GsonBuilder()
        // gbuilder.setPrettyPrinting()
        // def json = gbuilder.create().toJson(cfgs)

        // outfile.parentFile.mkdirs()
        // outfile.text = json
    }
}
