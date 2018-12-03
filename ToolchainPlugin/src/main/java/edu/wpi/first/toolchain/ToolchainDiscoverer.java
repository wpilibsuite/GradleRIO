package edu.wpi.first.toolchain;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativeplatform.toolchain.internal.SystemLibraries;
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.GccMetadata;
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.GccMetadataProvider;
import org.gradle.platform.base.internal.toolchain.SearchResult;
import org.gradle.process.ExecSpec;
import org.gradle.process.internal.DefaultExecActionFactory;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.util.TreeVisitor;
import org.gradle.util.VersionNumber;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ToolchainDiscoverer implements Named {

    private String name;
    private GccMetadataProvider metadataProvider;
    private Optional<GccMetadata> metadataLazy;
    private Optional<File> rootDir;
    private String versionLo, versionHi;
    private Function<String, String> composer;

    public ToolchainDiscoverer(String name, File rootDir, Function<String, String> composer) {
        this.name = name;
        this.rootDir = optFile(rootDir);
        this.versionLo = null;
        this.versionHi = null;
        this.composer = composer;
        this.metadataLazy = Optional.empty();

        FileResolver fileResolver = new IdentityFileResolver();
        ExecActionFactory execFactory = new DefaultExecActionFactory(fileResolver);
        this.metadataProvider = GccMetadataProvider.forGcc(execFactory);
    }

    public void configureVersions(String low, String high) {
        this.versionLo = low;
        this.versionHi = high;
    }

    public Optional<File> rootDir() {
        return rootDir;
    }

    public boolean exists() {
        return metadata(null).isPresent();
    }

    public boolean versionValid() {
        if (!exists()) return false;

        VersionNumber v = metadata(null).get().getVersion();
        boolean loValid = versionLo == null || v.compareTo(VersionNumber.parse(versionLo)) >= 0;
        boolean hiValid = versionHi == null || v.compareTo(VersionNumber.parse(versionHi)) <= 0;

        return loValid && hiValid;
    }

    public boolean valid() {
        return exists() && versionValid();
    }

    public Optional<File> binDir() {
        return join(rootDir(), "bin");
    }

    public Optional<File> libDir() {
        return join(rootDir(), "lib");
    }

    public Optional<File> includeDir() {
        return join(rootDir(), "include");
    }

    public Optional<File> tool(String tool) {
        return join(binDir(), toolName(tool));
    }

    public String toolName(String tool) {
        return composer == null ? tool : composer.apply(tool);
    }

    public Optional<File> gccFile() {
        return tool("g++");
    }

    public Optional<File> gdbFile() {
        return tool("gdb");
    }

    public Optional<File> sysroot() {
        return rootDir();
    }

    public Optional<GccMetadata> metadata(TreeVisitor<String> visitor) {
        if (!metadataLazy.isPresent())
            metadataLazy = metadata(gccFile().orElse(null), visitor);
        return metadataLazy;
    }

    public void explain(TreeVisitor<String> visitor) {
        visitor.node("Valid?: " + valid());
        visitor.node("Found?: " + exists());
        visitor.node("Version Range");
        visitor.startChildren();
        visitor.node("Low: " + versionLo);
        visitor.node("High: " + versionHi);
        visitor.node("Is Valid?: " + versionValid());
        visitor.endChildren();

        visitor.node("Root: " + rootDir().orElse(null));
        visitor.node("Bin: " + binDir().orElse(null));
        visitor.node("Lib: " + libDir().orElse(null));
        visitor.node("Include: " + includeDir().orElse(null));
        visitor.node("Gcc: " + gccFile().orElse(null));
        visitor.node("Gdb: " + gdbFile().orElse(null));

        if (exists()) {
            GccMetadata meta = metadata(null).get();

            visitor.node("Metadata");
            visitor.startChildren();
            visitor.node("Version: " + meta.getVersion().toString());
            visitor.node("Vendor: " + meta.getVendor());
            visitor.node("Default Arch: " + meta.getDefaultArchitecture().toString());

            visitor.node("System Libraries");
            visitor.startChildren();
            SystemLibraries syslib = meta.getSystemLibraries();
            visitor.node("Include");
            visitor.startChildren();
            for (File f : syslib.getIncludeDirs()) {
                visitor.node(f.getAbsolutePath());
            }
            visitor.endChildren();

            visitor.node("Lib Dirs");
            visitor.startChildren();
            for (File f : syslib.getLibDirs()) {
                visitor.node(f.getAbsolutePath());
            }
            visitor.endChildren();

            visitor.node("Macros");
            visitor.startChildren();
            for (Map.Entry<String, String> e : syslib.getPreprocessorMacros().entrySet()) {
                visitor.node(e.getKey() + " = " + e.getValue());
            }
            visitor.endChildren();
            visitor.endChildren(); // System Libraries
            visitor.endChildren(); // Metadata
        } else {
            if (gccFile().isPresent()) {
                visitor.node("Metadata Explain: ");
                visitor.startChildren();
                metadata(visitor);
                visitor.endChildren();
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private Optional<GccMetadata> metadata(File file, TreeVisitor<String> visitor) {
        if (file == null || !file.exists())
            return Optional.empty();
        SearchResult<GccMetadata> searchresult = metadataProvider.getCompilerMetaData(file, new ArrayList<String>(), new ArrayList<File>());
        if (visitor != null)
            searchresult.explain(visitor);
        return Optional.of(searchresult.getComponent());
    }

    public static List<File> systemPath(Project project, Function<String, String> composer) {
        String tool = composer == null ? "g++" : composer.apply("gcc");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        project.exec((ExecSpec spec) -> {
            spec.commandLine(OperatingSystem.current().isWindows() ? "where.exe" : "which", tool);
            spec.setStandardOutput(os);
            spec.setIgnoreExitValue(true);
        });

        return Arrays.stream(os.toString().trim().split("\n"))
                .map(String::trim)
                .filter(((Predicate<String>)String::isEmpty).negate())
                .map((String path) -> { return new File(path).getParentFile().getParentFile(); })
                .collect(Collectors.toList());
    }

    public static List<ToolchainDiscoverer> forSystemPath(Project project, Function<String, String> composer) {
        List<ToolchainDiscoverer> disc = new ArrayList<>();
        int i = 0;
        for (File f : systemPath(project, composer)) {
            disc.add(new ToolchainDiscoverer("Path" + (i++), f, composer));
        }
        return disc;
    }

    private static Optional<File> join(Optional<File> f, String join) {
        return optFile((File)(f.map((File file) -> { return new File(file, join); }).orElse(null)));
    }

    private static Optional<File> optFile(File f) {
        return (f == null || !f.exists()) ? Optional.empty() : Optional.of(f);
    }
}
