package edu.wpi.first.gradlerio;

import org.gradle.api.provider.Provider;

public interface SingletonTask {
  public Provider<String> getSingletonName();
}
