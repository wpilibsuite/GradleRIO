package edu.wpi.first.toolchain;

import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;

interface ToolchainRegistrarBase {
  void register(ToolchainOptions options, NativeToolChainRegistryInternal registry, Instantiator instantiator);
}
