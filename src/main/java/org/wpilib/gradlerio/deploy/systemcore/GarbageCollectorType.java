package org.wpilib.gradlerio.deploy.systemcore;

import java.util.List;

public enum GarbageCollectorType {
    G1("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=1", "-XX:GCTimeRatio=1"),
    G1_LongPause("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=5", "-XX:GCTimeRatio=1"),
    G1_Base("-XX:+UseG1GC"),
    Serial("-XX:+UseSerialGC"),
    Parallel("-XX:+UseParallelGC"),
    Serial_PauseGoal("-XX:+UseSerialGC", "-XX:MaxGCPauseMillis=5"),
    Parallel_PauseGoal("-XX:+UseParallelGC", "-XX:MaxGCPauseMillis=5"),
    Other();

    private final List<String> gcArguments;

    public List<String> getGcArguments() {
        return gcArguments;
    }

    private GarbageCollectorType(String... arguments) {
        gcArguments = List.of(arguments);
    }
}
