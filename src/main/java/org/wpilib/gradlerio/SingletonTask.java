package org.wpilib.gradlerio;

import org.gradle.api.provider.Provider;

public interface SingletonTask {
    public Provider<String> getSingletonName();
}
