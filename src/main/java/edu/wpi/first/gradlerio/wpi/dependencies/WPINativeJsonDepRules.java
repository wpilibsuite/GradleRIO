package edu.wpi.first.gradlerio.wpi.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.PlatformContainer;

import edu.wpi.first.embeddedtools.nativedeps.NativeDepsSpec;
import edu.wpi.first.embeddedtools.nativedeps.NativeLib;
import edu.wpi.first.gradlerio.wpi.WPIExtension;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension.CppArtifact;
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension.JsonDependency;
import edu.wpi.first.toolchain.NativePlatforms;

public class WPINativeJsonDepRules extends RuleSource {
    @Mutate
    public void addJsonLibraries(NativeDepsSpec libs, final PlatformContainer platformContainer, final ExtensionContainer extensionContainer) {
        WPIExtension wpi = extensionContainer.getByType(WPIExtension.class);
        Action<NativeLib> common = lib -> {
            lib.setHeaderDirs(List.of());
            lib.setSourceDirs(List.of());
            lib.setStaticMatchers(List.of());
            lib.setDebugMatchers(List.of("**/*.pdb", "**/*.so.debug"));
            lib.setSharedMatchers(List.of());
            lib.setDynamicMatchers(List.of());
        };

        for(JsonDependency dep : wpi.getDeps().getVendor().getDependencies()) {
            for (CppArtifact cpp : dep.cppDependencies) {
                String linkSuff = cpp.sharedLibrary ? "" : "static";
                String name = dep.uuid + cpp.libName;
                String mavenbase = cpp.groupId + ":" + cpp.artifactId + ":" + WPIVendorDepsExtension.getVersion(cpp.version, wpi);
                String config = cpp.configuration != null ? cpp.configuration : "native_" + dep.uuid + "_" + cpp.groupId + cpp.artifactId;
                List<String> allPlatforms = platformContainer.stream().map(x -> x.getName()).collect(Collectors.toList());

                // Note: because of a discrepancy between the target platforms of the headers, sources
                // and the binaries, we don't provide a CombinedNativeLib, meaning all of _binaries,
                // _headers and _sources must be applied. We do this in WPIVendorDepsExtension#cppVendorLibForBin

                if (cpp.headerClassifier != null) {
                    libs.create(name + "_headers", NativeLib.class, lib -> {
                        common.execute(lib);

                        lib.setTargetPlatforms(allPlatforms);
                        lib.getHeaderDirs().add("");
                        lib.setLibraryName(name + "_headers");
                        lib.setMaven(mavenbase + ":" + cpp.headerClassifier + "@zip");
                        lib.setConfiguration(config + "_headers");
                    });
                }

                if (cpp.sourcesClassifier != null) {
                    libs.create(name + "_sources", NativeLib.class, lib -> {
                        common.execute(lib);

                        lib.setTargetPlatforms(allPlatforms);
                        lib.getSourceDirs().add("");
                        lib.setLibraryName(name + "_sources");
                        lib.setMaven(mavenbase + ":" + cpp.sourcesClassifier + "@zip");
                        lib.setConfiguration(config + "_sources");
                    });
                }

                List<String> buildKinds = List.of("debug", "");

                for (String buildKind : buildKinds) {
                    String buildType = buildKind.contains("debug") ? "debug" : "release";
                    String binaryConfig = config + buildKind;

                    if (cpp.binaryPlatforms != null) {
                        if (Arrays.asList(cpp.binaryPlatforms).contains(NativePlatforms.roborio)) {
                            String platform = NativePlatforms.roborio;
                            libs.create(name + "_" + platform + buildKind, NativeLib.class, lib -> {
                                common.execute(lib);
                                lib.setTargetPlatforms(List.of(platform));
                                lib.setLibraryName(name + "_binaries");
                                lib.setBuildType(buildType);
                                if (cpp.sharedLibrary) {
                                    lib.setSharedMatchers(List.of("**/*" + cpp.libName + "*.so"));
                                    lib.setDynamicMatchers(lib.getSharedMatchers());
                                } else {
                                    lib.setStaticMatchers(List.of("**/*" + cpp.libName + "*.a"));
                                }
                                lib.setMaven(mavenbase + ":" + platform + linkSuff + buildKind + "@zip");
                                // It can't be 'config' otherwise missing libs break even if not used!
                                lib.setConfiguration(binaryConfig + "_" + platform);
                            });
                        }

                        for (String platform : cpp.binaryPlatforms) {
                            // Skip athena, as it is specifically handled
                            if (platform.contains(NativePlatforms.roborio)) {
                                continue;
                            }

                            libs.create(name + "_" + platform + buildKind, NativeLib.class, lib -> {
                                common.execute(lib);
                                lib.setTargetPlatforms(List.of(platform));
                                lib.setLibraryName(name + "_binaries");

                                lib.setBuildType(buildType);

                                List<String> staticMatchers = new ArrayList<>();
                                staticMatchers.add("**/*" + cpp.libName + "*.lib");
                                lib.setStaticMatchers(staticMatchers);
                                if (cpp.sharedLibrary) {
                                    lib.setSharedMatchers(List.of("**/*" + cpp.libName + "*.so", "**/*" + cpp.libName + "*.dylib"));
                                    List<String> dynamicMatchers = new ArrayList<>(lib.getSharedMatchers());
                                    dynamicMatchers.add("**/" + cpp.libName + "*.dll");
                                    lib.setDynamicMatchers(dynamicMatchers);
                                } else {
                                    lib.getStaticMatchers().add("**/*" + cpp.libName + "*.a");
                                }

                                lib.setMaven(mavenbase + ":" + platform + linkSuff + buildType + "@zip");
                                // It can't be 'config' otherwise missing libs break even if not used!
                                lib.setConfiguration(binaryConfig + "_" + platform);
                            });
                        }
                    }
                }
            }
        }
    }
}
