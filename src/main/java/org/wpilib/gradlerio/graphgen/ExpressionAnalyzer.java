package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SwitchExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ExpressionAnalyzer {
    /**
     * Analyzes an inline expression for returned state machine states,
     * and the conditions needed for the state to be returned.
     * @param expr The expression to analyze
     * @return A map of state names to the conditions needed for that state to be returned
     */
    static Map<String, String> analyze(Expression expr) {
        var rawReturns = new HashMap<String, List<String>>();
        ExpressionAnalyzer.analyze(expr, "true", rawReturns);
        return rawReturns.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> Utils.joinWithOr(entry.getValue())
            ));
    }

    // A mutable variant of analyze() used internally by CodeBlockAnalyzer.
    static void analyze(
        Expression expr,
        String pathCondition,
        Map<String, List<String>> returnsMap
    ) {
        if (expr.isSwitchExpr()) {
            handleSwitch(expr.asSwitchExpr(), pathCondition, returnsMap);
        } else if (expr.isConditionalExpr()) {
            var conditional = expr.asConditionalExpr();
            var condition = conditional.getCondition().toString();
            analyze(conditional.getThenExpr(), Utils.joinWithAnd(pathCondition, condition), returnsMap);
            analyze(conditional.getElseExpr(), Utils.joinWithAnd(pathCondition, Utils.negate(condition)), returnsMap);
        } else {
            returnsMap.computeIfAbsent(expr.toString(), k -> new ArrayList<>()).add(pathCondition);
        }
    }

    private static void handleSwitch(
        SwitchExpr sw,
        String pathCondition,
        Map<String, List<String>> returnsMap
    ) {
        var selector = sw.getSelector().toString();
        var entries = sw.getEntries();
        var defaultClauses = new ArrayList<String>();
        for (var entry: entries) {
            String matchCondition;
            if (entry.getLabels().isEmpty()) {
                matchCondition = Utils.joinWithAnd(defaultClauses);
            } else {
                // One or more comma-separated labels (arrow style) or stacked case: lines
                matchCondition = Utils.joinWithOr(
                    entry.getLabels().stream().map(label -> selector + " == " + label).toList()
                );
                defaultClauses.add(Utils.negate(matchCondition));
            }

            var stmt = entry.getStatements().get(0);
            if (stmt.isBlockStmt()) {
                ErrorLogger.throwError("Yield Statements within switch blocks are not supported for @MakeStateMachineGraph.", stmt);
            }
            var expression = stmt.asExpressionStmt().getExpression();
            var condition = Utils.joinWithAnd(pathCondition, matchCondition);
            if (expression.isConditionalExpr() || expression.isSwitchExpr()) {
                analyze(expression, condition, returnsMap);
            } else {
                var varName = expression.toString().trim();
                returnsMap.computeIfAbsent(varName, k -> new ArrayList<>()).add(condition);
            }
        }
    }
}
