package edu.wpi.first.gradlerio.wpi


import edu.wpi.first.toolchain.NativePlatforms
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.internal.ProjectLayout
import org.gradle.language.nativeplatform.tasks.AbstractNativeSourceCompileTask
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.model.Validate
import org.gradle.nativeplatform.BuildTypeContainer
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.TargetedNativeComponent
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinarySpec
import org.gradle.platform.base.ComponentSpec
import org.gradle.platform.base.ComponentSpecContainer
import org.gradle.api.plugins.ExtensionContainer
import edu.wpi.first.nativeutils.NativeUtilsExtension

@CompileStatic
class WPINativeCompileRules extends RuleSource {


    @Mutate
    void addBuildTypes(BuildTypeContainer bts) {
        bts.maybeCreate('debug')
        bts.maybeCreate('release')
    }

    @Mutate
    void addBinaryFlags(BinaryContainer binaries, ExtensionContainer extensions) {
        def ntExt = extensions.getByType(NativeUtilsExtension)

        binaries.withType(NativeBinarySpec, { NativeBinarySpec bin ->
            ntExt.usePlatformArguments(bin)
        })
    }

    @Validate
    void setupCompilerWarningPrints(ModelMap<Task> tasks, ProjectLayout layout, ComponentSpecContainer components) {
        if (components == null) return;
        def project = (Project) layout.projectIdentifier

        for (ComponentSpec c : components) {
            if (c instanceof TargetedNativeComponent) {
                for (BinarySpec bin : ((TargetedNativeComponent)c).binaries) {
                    bin.tasks.withType(AbstractNativeSourceCompileTask) { AbstractNativeSourceCompileTask t ->
                        t.doLast {
                            printWarningsForBinTask(t.name.toString(), project)
                        }
                    }
                }
            }
        }
    }

    // From https://github.com/wpilibsuite/native-utils/blob/a8ea595670716c7b898878a37e36c2b52e8e3f42/src/main/groovy/edu/wpi/first/nativeutils/rules/BuildConfigRules.groovy#L450
    private static void printWarningsForBinTask(String taskName, Project project) {
        def file = new File(project.buildDir, "tmp/$taskName/output.txt")

        if (!file.exists()) return;

        def currentFile = ''
        def hasFirstLine = false
        def hasPrintedFileName = false

        file.eachLine { line ->
            if (!hasFirstLine) {
                hasFirstLine = true
            } else if (line.startsWith('compiling ')) {
                currentFile = line.substring(10, line.indexOf('successful.'))
                hasPrintedFileName = false
            } else if (line.contains('Finished') && line.contains('see full log')) {
                // No op
            } else if (line.trim().equals(currentFile.trim())) {
                // No op
            } else if (!line.isEmpty()) {
                if (!hasPrintedFileName) {
                    hasPrintedFileName = true
                    System.out.println("Warnings in file $currentFile....")
                }
                System.out.println(line)
            }
        }
    }
}
