package edu.wpi.first.toolchain.configurable;

import edu.wpi.first.toolchain.ToolchainDescriptor;

public class DefaultCrossCompilerConfiguration implements CrossCompilerConfiguration {
  private final String name;
  private String architecture;
  private String operatingSystem;
  private String compilerPrefix;
  private boolean optional;
  private ToolchainDescriptor descriptor;

  public DefaultCrossCompilerConfiguration(String name) {
    this.name = name;
  }

  public DefaultCrossCompilerConfiguration(String name, ToolchainDescriptor descriptor) {
    this.name = name;
    this.descriptor = descriptor;
  }

  public String getName() {
    return name;
  }

  /**
   * @return the architecture
   */
  public String getArchitecture() {
    return architecture;
  }

  /**
   * @param architecture the architecture to set
   */
  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  /**
   * @return the operatingSystem
   */
  public String getOperatingSystem() {
    return operatingSystem;
  }

  /**
   * @param operatingSystem the operatingSystem to set
   */
  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  /**
   * @return the compilerPrefix
   */
  public String getCompilerPrefix() {
    return compilerPrefix;
  }

  /**
   * @param compilerPrefix the compilerPrefix to set
   */
  public void setCompilerPrefix(String compilerPrefix) {
    this.compilerPrefix = compilerPrefix;
  }

  @Override
  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  @Override
  public boolean getOptional() {
    return optional;
  }

  @Override
  public void setToolchainDescriptor(ToolchainDescriptor optional) {
    this.descriptor = optional;
  }

  @Override
  public ToolchainDescriptor getToolchainDescriptor() {
    return descriptor;
  }
}
