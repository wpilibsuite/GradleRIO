package edu.wpi.first.gradlerio.ide

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.Delete
import org.gradle.language.base.plugins.ComponentModelBasePlugin

class KDevelopPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(ComponentModelBasePlugin)
        project.extensions.create('kdevelop', EditorConfigurationExtension)

        project.tasks.register('kdevelop', GenerateKDevelopTask) { GenerateKDevelopTask task ->
            task.group = "GradleRIO"
            task.description = "Generate KDevelop4 IDE Files."
        }

        project.tasks.register('cleanKDevelop', Delete) { Delete task ->
            task.group = "GradleRIO"
            task.description = "Clean KDevelop4 IDE Files."
            [".kdev4", "${project.name}.kdev4"].each {String file ->
                def f = new File(file)
                if (f.exists()) task.delete(f)
            }
        }
    }
}
