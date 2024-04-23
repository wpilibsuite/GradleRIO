package edu.wpi.first.gradlerio.simulation;

public class HalSimPair {
  public final String name;
  public final String libName;
  public final boolean defaultEnabled;

  public HalSimPair(String name, String libName, boolean defaultEnabled) {
    this.name = name;
    this.libName = libName;
    this.defaultEnabled = defaultEnabled;
  }
}
