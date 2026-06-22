package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.ast.Node;

public class ErrorLogger {
    private static String currentFilePath = "unknown file";

    static void setFilePath(String path) {
        currentFilePath = path;
    }

    /**
     * A utility method that logs the line number and the file path
     * that an error originates from, in addition to the error message.
     * This serves to supplement the lack of stacktrace support within JavaParser.
     * @param message The error message to throw
     * @param node The location (statement or expression) that the error originates from
     */
    static void throwError(String message, Node node) {
        var begin = node.getBegin();
        if (begin.isPresent()) {
            throw new RuntimeException(currentFilePath + ": Line " + begin.get().line + ": " + message);
        } else {
            throw new RuntimeException(currentFilePath + ": Line " + message);
        }
    }
}
