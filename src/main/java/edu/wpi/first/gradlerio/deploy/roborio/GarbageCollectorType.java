package edu.wpi.first.gradlerio.deploy.roborio;

import java.util.List;

public enum GarbageCollectorType {
    CMS("-XX:+UseConcMarkSweepGC"),
    G1("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=1", "-XX:GCTimeRatio=1"),
    Other();
    
    private final List<String> gcArguments;

    public List<String> getGcArguments() {
        return gcArguments;
    }

    private GarbageCollectorType(String... arguments) {
        gcArguments = List.of(arguments);
    }
}
