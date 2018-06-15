package jaci.openrio.gradle.wpi.toolchain.discover

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.openrio.gradle.GradleRIOPlugin
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.GccMetadata
import org.gradle.process.ExecSpec
import org.gradle.util.TreeVisitor
import org.gradle.util.VersionNumber

@InheritConstructors
@CompileStatic
class SystemPathToolchainDiscoverer extends AbstractToolchainDiscoverer {

    private Optional<File> rootDirLazy = Optional.empty()
    private List<File> ignoredCandidates = []

    @Override
    Optional<File> rootDir() {
        if (!rootDirLazy.isPresent()) {
            ignoredCandidates = []
            def os = new ByteArrayOutputStream()
            project.exec { ExecSpec spec ->
                def tool = composeTool("g++")
                if (OperatingSystem.current().isWindows())
                    spec.commandLine("where.exe", tool)
                else
                    spec.commandLine("which", tool)

                spec.standardOutput = os
                spec.ignoreExitValue = true
            }
            def paths = os.toString().trim().split("\n").findAll { String s -> !s.trim().isEmpty() } as List<String>
            def candidates = [] as ArrayList<File>
            def bestVersion = null as VersionNumber
            def bestCandidate = null as File

            // Find toolchain with correct version
            paths.each { String path ->
                File f = new File(path.trim())
                def meta = metadata(f)
                if (meta.isPresent() && f.parentFile != null && f.parentFile.parentFile != null) {
                    candidates << f
                    def vers = meta.get().version

                    // 2018 - GCC 5.5
                    if (bestVersion == null && vers.major == 5 && vers.minor == 5) {
                        bestVersion = vers
                        bestCandidate = f
                    }
                } else {
                    ignoredCandidates << f
                }
            }

            candidates.each { File f ->
                if (!f.equals(bestCandidate))
                    ignoredCandidates << f
                else
                    rootDirLazy = optFile(f.parentFile.parentFile)
            }
        }
        return rootDirLazy
    }

    @Override
    void explain(TreeVisitor<? extends String> visitor) {
        super.explain(visitor)
        visitor.with {
            if (ignoredCandidates.size() > 0) {
                node("Ignored Candidates")
                startChildren()
                ignoredCandidates.each { File f ->
                    node(f.absolutePath)
                    startChildren()
                    def meta = metadata(f)
                    if (!meta.isPresent())
                        node("File does not exist!")
                    else if (f.parentFile == null || f.parentFile.parentFile == null)
                        node("Toolchain is missing parents!")
                    else {
                        node("Version: " + meta.get().version.toString())
                    }
                    endChildren()
                }
                endChildren()
            } else {
                node("No ignored candidates")
            }
        }
    }

}
