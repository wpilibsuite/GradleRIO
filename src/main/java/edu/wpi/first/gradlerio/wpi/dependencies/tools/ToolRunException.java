package edu.wpi.first.gradlerio.wpi.dependencies.tools;

public class ToolRunException extends RuntimeException {
    private static final long serialVersionUID = 8095609598836161230L;

    public ToolRunException(String stdOut, String stdErr) {
        super("Tool failed to start:\n\nOutput: " + stdOut + "\n\nError: " + stdErr);
    }
}
