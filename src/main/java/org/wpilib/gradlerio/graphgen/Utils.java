package org.wpilib.gradlerio.graphgen;

class Utils {
    public static String addParentheses(String condition) {
        if ((condition.startsWith("!(") || condition.startsWith("(")) && condition.endsWith(")")) {
            return condition;
        }
        return "(" + condition + ")";
    }
}
