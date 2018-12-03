package edu.wpi.first.gradlerio.wpi

import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.toolchain.roborio.RoboRioGcc
import groovy.transform.CompileStatic
import jaci.gradle.log.ETLoggerFactory
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.BuildTypeContainer
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryTasks
import org.gradle.process.ExecSpec

@CompileStatic
class WPINativeCompileRules extends RuleSource {

    // Rules require this to be explicitly marked public static final
    public static final String[] windowsCompilerArgs = ['/EHsc', '/DNOMINMAX', '/Zi', '/FS', '/Zc:inline']
    public static final String[] windowsCCompilerArgs = ['/Zi', '/FS', '/Zc:inline']
    public static final String[] windowsReleaseCompilerArgs = ['/O2', '/MD']
    public static final String[] windowsDebugCompilerArgs = ['/Od', '/MDd']
    public static final String[] windowsLinkerArgs = ['/DEBUG:FULL']
    public static final String[] windowsReleaseLinkerArgs = ['/OPT:REF', '/OPT:ICF']

    public static final String[] linuxCrossCompilerArgs = ['-std=c++14', '-Wformat=2', '-pedantic', '-Wno-psabi', '-g',
                                                           '-Wno-unused-parameter', '-Wno-error=deprecated-declarations', '-fPIC', '-rdynamic',
                                                           '-pthread']
    public static final String[] linuxCrossCCompilerArgs = ['-Wformat=2', '-Wno-psabi', '-g',
                                                            '-Wno-unused-parameter', '-fPIC', '-rdynamic', '-pthread']
    public static final String[] linuxCrossLinkerArgs = ['-rdynamic', '-pthread', '-ldl']
    public static final String[] linuxCrossReleaseCompilerArgs = ['-O2']
    public static final String[] linuxCrossDebugCompilerArgs = ['-Og']

    public static final String[] linuxCompilerArgs = ['-std=c++14', '-Wformat=2', '-pedantic', '-Wno-psabi', '-g',
                                                      '-Wno-unused-parameter', '-Wno-error=deprecated-declarations', '-fPIC', '-rdynamic',
                                                      '-pthread']
    public static final String[] linuxCCompilerArgs = ['-Wformat=2', '-pedantic', '-Wno-psabi', '-g',
                                                       '-Wno-unused-parameter', '-fPIC', '-rdynamic', '-pthread']
    public static final String[] linuxLinkerArgs = ['-rdynamic', '-pthread', '-ldl']
    public static final String[] linuxReleaseCompilerArgs = ['-O2']
    public static final String[] linuxDebugCompilerArgs = ['-O0']

    public static final String[] macCompilerArgs = ['-std=c++14', '-pedantic-errors', '-fPIC', '-g',
                                                    '-Wno-unused-parameter', '-Wno-error=deprecated-declarations', '-Wno-missing-field-initializers',
                                                    '-Wno-unused-private-field', '-Wno-unused-const-variable', '-pthread']
    public static final String[] macCCompilerArgs = ['-pedantic-errors', '-fPIC', '-g',
                                                     '-Wno-unused-parameter', '-Wno-missing-field-initializers', '-Wno-unused-private-field']
    public static final String[] macObjCppLinkerArgs = ['-std=c++14', '-stdlib=libc++','-fobjc-arc', '-g', '-fPIC']
    public static final String[] macReleaseCompilerArgs = ['-O2']
    public static final String[] macDebugCompilerArgs = ['-O0']
    public static final String[] macLinkerArgs = ['-framework', 'CoreFoundation', '-framework', 'AVFoundation', '-framework', 'Foundation', '-framework', 'CoreMedia', '-framework', 'CoreVideo']

    @BinaryTasks
    void createNativeStripTasks(final ModelMap<Task> tasks, final ExtensionContainer extContainer, final NativeBinarySpec binary) {
        final Project project = extContainer.getByType(GradleRIOPlugin.ProjectWrapper).project
        RoboRioGcc gcc = null
        if (binary.toolChain instanceof RoboRioGcc) {
            gcc = (RoboRioGcc)binary.toolChain
        } else {
            return
        }
        Task rawLinkTask = null
        if (binary instanceof SharedLibraryBinarySpec) {
            rawLinkTask = ((SharedLibraryBinarySpec)binary).tasks.link
        } else if (binary instanceof NativeExecutableBinarySpec) {
            rawLinkTask = ((NativeExecutableBinarySpec)binary).tasks.link
        }
        if (!(rawLinkTask instanceof AbstractLinkTask)) {
            return
        }
        AbstractLinkTask linkTask = (AbstractLinkTask)rawLinkTask

        linkTask.doLast {
            def mainFile = linkTask.linkedFile.get().asFile

            if (mainFile.exists()) {
                def mainFileStr = mainFile.toString()
                def debugFile = mainFileStr + '.debug'

                def disc = gcc.discoverer
                def binDir = disc.binDir().get().toString()

                def objcopyOptional = disc.tool('objcopy')
                def stripOptional = disc.tool('strip')
                if (!objcopyOptional.isPresent() || !stripOptional.isPresent()) {
                    def logger = ETLoggerFactory.INSTANCE.create("NativeBinaryStrip")
                    logger.logError('Failed to strip binaries because of unknown tool objcopy and strip')
                    return
                }

                def objcopy = disc.tool('objcopy').get().toString()
                def strip = disc.tool('strip').get().toString()

                project.exec { ExecSpec ex ->
                    ex.commandLine objcopy, '--only-keep-debug', mainFileStr, debugFile
                }
                project.exec { ExecSpec ex ->
                    ex.commandLine strip, '-g', mainFileStr
                }
                project.exec { ExecSpec ex ->
                    ex.commandLine objcopy, "--add-gnu-debuglink=$debugFile", mainFileStr
                }
            }
        }
    }

    @Mutate
    void addBuildTypes(BuildTypeContainer bts) {
        bts.maybeCreate('debug')
        bts.maybeCreate('release')
    }

    @Mutate
    void addBinaryFlags(BinaryContainer binaries) {
        binaries.withType(NativeBinarySpec, { NativeBinarySpec bin ->
            if (bin.targetPlatform.operatingSystem.isWindows()) {
                // Windows
                windowsCompilerArgs.each { String arg ->
                    bin.cppCompiler.args << arg
                }
                windowsCCompilerArgs.each { String arg ->
                    bin.cCompiler.args << arg
                }
                windowsLinkerArgs.each { String arg ->
                    bin.linker.args << arg
                }
                if (bin.buildType.name.contains('debug')) {
                    windowsDebugCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                } else {
                    windowsReleaseCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                    windowsReleaseLinkerArgs.each { String arg ->
                        bin.linker.args << arg
                    }
                }
            } else if (bin.targetPlatform.operatingSystem.isMacOsX()) {
                // OSX
                macCompilerArgs.each { String arg ->
                    bin.cppCompiler.args << arg
                }
                macCCompilerArgs.each { String arg ->
                    bin.cCompiler.args << arg
                }
                macObjCppLinkerArgs.each { String arg ->
                    bin.objcppCompiler.args << arg
                }
                macLinkerArgs.each { String arg ->
                    bin.linker.args << arg
                }
                if (bin.buildType.name.contains('debug')) {
                    macDebugCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                        bin.objcCompiler.args << arg
                        bin.objcppCompiler.args << arg
                    }
                } else {
                    macReleaseCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                        bin.objcCompiler.args << arg
                        bin.objcppCompiler.args << arg
                    }
                }
            } else if (bin.toolChain.name.equals('roborioGcc')) {
                // Rio
                linuxCrossCompilerArgs.each { String arg ->
                    bin.cppCompiler.args << arg
                }
                linuxCrossCCompilerArgs.each { String arg ->
                    bin.cCompiler.args << arg
                }
                linuxCrossLinkerArgs.each { String arg ->
                    bin.linker.args << arg
                }
                if (bin.buildType.name.contains('debug')) {
                    linuxCrossDebugCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                } else {
                    linuxCrossReleaseCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                }
            } else {
                // Linux
                linuxCompilerArgs.each { String arg ->
                    bin.cppCompiler.args << arg
                }
                linuxCCompilerArgs.each { String arg ->
                    bin.cCompiler.args << arg
                }
                linuxLinkerArgs.each { String arg ->
                    bin.linker.args << arg
                }
                if (bin.buildType.name.contains('debug')) {
                    linuxDebugCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                } else {
                    linuxReleaseCompilerArgs.each { String arg ->
                        bin.cppCompiler.args << arg
                        bin.cCompiler.args << arg
                    }
                }
            }

            if (bin.buildType.name.equals('debug')) {
                bin.cppCompiler.define('DEBUG')
            }
            null
        } as Action<? extends NativeBinarySpec>)
    }
}
