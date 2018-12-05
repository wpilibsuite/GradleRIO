package edu.wpi.first.toolchain;

import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.work.WorkerLeaseService;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery;
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory;
import org.gradle.process.internal.ExecActionFactory;

public class ToolchainOptions {

    public String name;
    public Project project;
    public ToolchainDescriptor descriptor;

    Instantiator instantiator;
    BuildOperationExecutor buildOperationExecutor;
    OperatingSystem operatingSystem;
    FileResolver fileResolver;
    ExecActionFactory execActionFactory;
    CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory;
    CompilerMetaDataProviderFactory metaDataProviderFactory;
    WorkerLeaseService workerLeaseService;
    SystemLibraryDiscovery systemLibraryDiscovery;

    public ToolchainOptions(Instantiator inst, BuildOperationExecutor boe, OperatingSystem os, FileResolver fr,
                            ExecActionFactory eaf, CompilerOutputFileNamingSchemeFactory cofnsf,
                            CompilerMetaDataProviderFactory cmdpf, WorkerLeaseService wls, SystemLibraryDiscovery sld) {
        this.instantiator = inst;
        this.buildOperationExecutor = boe;
        this.operatingSystem = os;
        this.fileResolver = fr;
        this.execActionFactory = eaf;
        this.compilerOutputFileNamingSchemeFactory = cofnsf;
        this.metaDataProviderFactory = cmdpf;
        this.workerLeaseService = wls;
        this.systemLibraryDiscovery = sld;
    }

}
