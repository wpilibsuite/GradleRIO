package edu.wpi.first.gradlerio.ide

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Delete
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.platform.base.BinaryContainer

class ClionPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(ComponentModelBasePlugin)
        project.extensions.create('clion', ClionExtension)

        project.tasks.register('cleanClion', Delete) { Delete task ->
            task.group = "GradleRIO"
            task.description = "Clean Clion IDE Files"
            ['.idea', 'cmake-build-debug', 'CMakeLists.txt'].each { String file ->
                def f = new File(file)
                if (f.exists()) task.delete(f)
            }
        }

        project.tasks.register('clion', GenerateClionTask) { GenerateClionTask task ->
            task.group = "GradleRIO"
            task.description = "Generate Clion CMakeLists.txt files."
        }
    }

    static class ClionRules extends RuleSource {
        @Mutate
        void addClionBinaries(ModelMap<Task> tasks, BinaryContainer binaries, ExtensionContainer exts) {
            binaries.withType(NativeExecutableBinarySpec).each { NativeExecutableBinarySpec spec ->
                exts.getByType(ClionExtension)._binaries << new ClionExtension.ClionBinarySpec(spec, spec.executable.file)
            }
            binaries.withType(SharedLibraryBinarySpec).each { SharedLibraryBinarySpec spec ->
                exts.getByType(ClionExtension)._binaries << new ClionExtension.ClionBinarySpec(spec, spec.sharedLibraryFile)
            }
        }
    }
}
