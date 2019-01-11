package edu.wpi.first.gradlerio.ide

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.nativeplatform.HeaderExportingSourceSet
import org.gradle.nativeplatform.NativeDependencySet

import java.nio.file.Paths

@CompileStatic
class GenerateClionTask extends DefaultTask {

    String relative(File base, File file) {
        return Paths.get(base.path).relativize(Paths.get(file.path)).toString().replaceAll("\\\\", "/")
    }

    @TaskAction
    void generate() {
        println "WARNING: Clion Support is very hacky, and is achieved through a fake CMakeLists.txt file"
        println "         as Clion itself does not support gradle/'just from source' builds. On windows, you must"
        println "         enable the clion.enable.msvc Clion Registry key and configure MSVC."

        def subdirs_write = false
        def file = new FileWriter(project.file('CMakeLists.txt'))
        file.write("cmake_minimum_required(VERSION 3.3)\n")
        file.write("project(${project.name})\nset(CMAKE_CXX_STANDARD 14)\n\n")

        project.extensions.getByType(ClionExtension)._binaries.each { ClionExtension.ClionBinarySpec clionBin ->
            def src_dirs = [] as Set<File>
            def include_dirs = [] as Set<File>
            def dep_include_dirs = [] as Set<File>

            def bin = clionBin.binary
            def binfile = clionBin.file

            bin.inputs.withType(HeaderExportingSourceSet) { HeaderExportingSourceSet ss ->
                src_dirs += ss.source.srcDirs
                include_dirs += ss.exportedHeaders.srcDirs
            }
            bin.libs.each { NativeDependencySet ds ->
                dep_include_dirs += ds.includeRoots
            }

            if (src_dirs.size() > 0) {
                def files_str = src_dirs.collect { File f -> '"' + relative(project.projectDir, f) + '/*.*"' }.join(" ")
                file.write("file(GLOB_RECURSE SOURCES ${files_str})\n")
            }
            if (include_dirs.size() > 0) {
                def rel_paths = include_dirs.collect { File f -> relative(project.projectDir, f) }
                def files_str = rel_paths.collect { '"' + it + '/*.*"' }.join(" ")
                file.write("file(GLOB_RECURSE INCLUDES ${files_str})\n")
                rel_paths.each {
                    file.write("include_directories(${it})\n")
                }
            }
            file.write('\n')

            if (dep_include_dirs.size() > 0) {
                def rel_paths = dep_include_dirs.collect { File f -> relative(project.projectDir, f) }
                def files_str = rel_paths.collect { '"' + it + '/*.*"' }.join(" ")
                file.write("file(GLOB_RECURSE DEP_INCLUDES ${files_str})\n")
                rel_paths.each {
                    file.write("include_directories(${it})\n")
                }
                file.write("set(ALL_INCLUDES \${INCLUDES} \${DEP_INCLUDES})\n")
            } else {
                file.write("set(ALL_INCLUDES \${INCLUDES})\n")
            }
            file.write("\n")
            file.write("add_executable(fake_${binfile.name}_${bin.buildType.name} \${SOURCES} \${ALL_INCLUDES})\n")

            if (!subdirs_write) {
                subdirs_write = true
                project.subprojects.each { Project proj ->
                    file.write("add_subdirectory(${relative(project.projectDir, proj.projectDir)})\n")
                }
            }

            file.write("\n")
            def gradle_path = relative(project.projectDir, project.rootProject.projectDir)
            def work_dir = "WORKING_DIRECTORY ../${gradle_path} "
            def proj_path = project == project.rootProject ? "" : "${project.path}:"
            def gradle_exe = OperatingSystem.current().isWindows() ? "gradlew.bat" : "./gradlew"
            file.write("add_custom_target(${binfile.name}_${bin.buildType.name}_build ${gradle_exe} ${proj_path}build ${work_dir}SOURCES \${SOURCES} \${ALL_INCLUDES})\n")
            file.write("add_custom_target(${binfile.name}_${bin.buildType.name}_deploy ${gradle_exe} ${proj_path}deploy ${work_dir}SOURCES \${SOURCES} \${ALL_INCLUDES})\n")
        }
        file.close()
    }

}
