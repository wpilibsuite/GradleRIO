package edu.wpi.first.gradlerio.wpi.toolchain

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.IdentityFileResolver
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.GccMetadata
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.GccMetadataProvider
import org.gradle.process.internal.DefaultExecActionFactory
import org.gradle.process.internal.ExecActionFactory
import org.gradle.util.TreeVisitor
import org.gradle.util.VersionNumber

@CompileStatic
class ToolchainDiscoverer {

    String name
    GccMetadataProvider metadataProvider
    Optional<GccMetadata> metadataLazy
    Optional<File> rootDir
    String versionLo, versionHi
    Project project

    ToolchainDiscoverer(String name, Project project, File rootDir) {
        this.name = name
        this.project = project
        this.rootDir = optFile(rootDir)
        this.versionLo = null
        this.versionHi = null
        this.metadataLazy = Optional.empty()
        FileResolver fileResolver = new IdentityFileResolver()
        ExecActionFactory execActionFactory = new DefaultExecActionFactory(fileResolver)
        this.metadataProvider = GccMetadataProvider.forGcc(execActionFactory)
    }

    void configureVersionChecking(String low, String high) {
        versionLo = low
        versionHi = high
    }

    Optional<File> rootDir() {
        return rootDir
    }

    boolean exists() {
        return metadata().isPresent()
    }

    boolean versionValid() {
        if (!exists())
            return false

        def v = metadata().get().version
        def lowValid = versionLo == null || v.compareTo(VersionNumber.parse(versionLo)) >= 0
        def highValid = versionHi == null || v.compareTo(VersionNumber.parse(versionHi)) <= 0
        return lowValid && highValid
    }

    boolean valid() {
        return exists() && versionValid()
    }

    Optional<File> binDir() {
        return join(rootDir(), "bin")
    }

    Optional<File> libDir() {
        return join(rootDir(), "lib")
    }

    Optional<File> includeDir() {
        return join(rootDir(), "include")
    }

    Optional<File> tool(String tool) {
        return join(binDir(), composeTool(tool))
    }

    Optional<File> gccFile() {
        return tool("g++")
    }

    Optional<File> gdbFile() {
        return tool("gdb")
    }

    Optional<File> sysroot() {
        if (OperatingSystem.current().isLinux())
            return Optional.empty()
        else if (OperatingSystem.current().isMacOsX())
            return rootDir().map { File f -> new File(f, "arm-frc-linux-gnueabi") } as Optional<File>
        else
            return rootDir()
    }

    Optional<GccMetadata> metadata(TreeVisitor<? extends String> visitor = null) {
        if (!metadataLazy.isPresent()) {
            metadataLazy = metadata(gccFile().orElse(null), visitor)
        }
        return metadataLazy
    }

    Optional<GccMetadata> metadata(File file, TreeVisitor<? extends String> visitor = null) {
        if (file == null || !file.exists())
            return Optional.empty()
        def searchresult = metadataProvider.getCompilerMetaData(file, [])
        if (visitor != null)
            searchresult.explain(visitor)
        return Optional.of(searchresult.component)
    }

    void explain(TreeVisitor<? extends String> visitor) {
        visitor.with {
            node("Valid?: " + valid())
            node("Found?: " + exists())
            node("Version Range")
            startChildren()
                node("Low: " + versionLo)
                node("High: " + versionHi)
                node("Is Valid?: " + versionValid())
            endChildren()

            node("Root: " + rootDir().orElse(null))
            node("Bin: " + binDir().orElse(null))
            node("Lib: " + libDir().orElse(null))
            node("Include: " + includeDir().orElse(null))
            node("Gcc: " + gccFile().orElse(null))
            node("Gdb: " + gdbFile().orElse(null))

            if (exists()) {
                def meta = metadata().get()

                node("Metadata")
                startChildren()
                    node("Version: " + meta.version.toString())
                    node("Vendor: " + meta.vendor)
                    node("Default Arch: " + meta.defaultArchitecture.toString())

                    node("System Libraries")
                    startChildren()
                        def syslib = meta.systemLibraries
                        node("Include")
                        startChildren()
                        syslib.includeDirs.each { File f ->
                            node(f.absolutePath)
                        }
                        endChildren()

                        node("Lib Dirs")
                        startChildren()
                        syslib.libDirs.each { File f ->
                            node(f.absolutePath)
                        }
                        endChildren()

                        node("Macros")
                        startChildren()
                        syslib.preprocessorMacros.each { String k, String v ->
                            node(k + " = " + v)
                        }
                        endChildren()
                    endChildren()    // System Libraries
                endChildren() // Metadata
            } else {
                if (gccFile().isPresent()) {
                    node("Metadata Explain: ")
                    startChildren()
                    metadata(visitor)
                    endChildren()
                }
            }
        }
    }

    static String prefix() {
        return "arm-frc-linux-gnueabi-"
    }

    static String suffix() {
        return OperatingSystem.current().isWindows() ? ".exe" : ""
    }

    static String composeTool(String name) {
        return prefix() + name + suffix()
    }

    protected static Optional<File> join(Optional<File> f, String join) {
        return optFile((File)(f.map({ File file -> new File(file, join) }).orElse(null)))
    }

    protected static Optional<File> optFile(File f) {
        if (f == null || !f.exists())
            return Optional.empty()
        return Optional.of(f)
    }
}
