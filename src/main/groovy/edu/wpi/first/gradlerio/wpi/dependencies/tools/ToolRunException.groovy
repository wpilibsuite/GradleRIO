package edu.wpi.first.gradlerio.wpi.dependencies.tools

import groovy.transform.CompileStatic

@CompileStatic
class ToolRunException extends RuntimeException {
    ToolRunException(String stdOut, String stdErr) {
        super("Tool failed to start:\n\nOutput: ${stdOut}\n\nError: ${stdErr}")
    }
}
