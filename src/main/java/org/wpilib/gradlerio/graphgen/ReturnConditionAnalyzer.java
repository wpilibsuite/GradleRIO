
package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Statically analyzes a lambda body and builds a map of: returnedVariableName -> condition string
 * that must be true for that return to execute.
 */
public class ReturnConditionAnalyzer {
    /**
     * Analyzes a lambda expression to determine
     *
     * @param expr  full source of a single lambda
     * @return LinkedHashMap preserving insertion order, keyed by returned variable name
     */
    public static Map<String, String> analyze(LambdaExpr expr) {
        Map<String, String> result = new LinkedHashMap<>();
        var body = expr.getBody();
        if (body.isBlockStmt()) {
            walkBlock(body.asBlockStmt().getStatements(), Collections.emptyList(), result);
        } else {
            walkStatement(body, Collections.emptyList(), result, true);
        }
        return result;
    }

    private static void walkBlock(
        List<Statement> stmts,
        List<String> pathConds,
        Map<String, String> result
    ) {
        for (Statement stmt : stmts) {
            walkStatement(stmt, pathConds, result);
        }
    }

    private static void walkStatement(
        Statement stmt,
        List<String> pathConds,
        Map<String, String> result
    ) {
        walkStatement(stmt, pathConds, result, false);
    }

    private static void walkStatement(
        Statement stmt,
        List<String> pathConds,
        Map<String, String> result,
        boolean isLambdaInline
    ) {
        if (stmt.isReturnStmt()) {
            handleReturn(stmt.asReturnStmt(), pathConds, result);
        } else if (stmt.isIfStmt()) {
            handleIf(stmt.asIfStmt(), pathConds, result);
        } else if (stmt.isSwitchStmt()) {
            handleSwitchStmt(stmt.asSwitchStmt(), pathConds, result);
        } else if (stmt.isYieldStmt()) {
            handleYield(stmt.asYieldStmt(), pathConds, result);
        } else if (stmt.isBlockStmt()) {
            walkBlock(stmt.asBlockStmt().getStatements(), pathConds, result);
        } else if (stmt.isExpressionStmt()) {
            var expr = stmt.asExpressionStmt().getExpression();
            if (expr.isConditionalExpr()) {
                handleInlineConditional(expr, pathConds, result);
            } else if (isLambdaInline) {
                result.put(expr.toString(), "");
            }
        } else if (!stmt.isEmptyStmt()) {
            // For any other statement type, descend into direct child statements
            stmt.findAll(Statement.class, s -> s != stmt && s.getParentNode().map(p -> p == stmt).orElse(false))
                    .forEach(child -> walkStatement(child, pathConds, result));
        }
    }

    private static void handleReturn(
        ReturnStmt ret,
        List<String> pathConds,
        Map<String, String> result
    ) {
        ret.getExpression().ifPresent(expr -> {
            if (expr.isSwitchExpr()) {
                // return switch (x) { case 1 -> a; ... }
                handleSwitchExpr(expr.asSwitchExpr(), pathConds, result);
            } else {
                String varName = expr.toString().trim();
                String condition = joinConditions(pathConds);
                mergeCondition(result, varName, condition.isEmpty() ? "true" : condition);
            }
        });
    }

    private static void handleInlineConditional(
        Expression expr,
        List<String> pathConds,
        Map<String, String> result
    ) {
        if (!expr.isConditionalExpr()) {
            mergeCondition(result, expr.toString().trim(), joinConditions(pathConds));
            return;
        }
        var conditional = expr.asConditionalExpr();
        String cond = conditional.getCondition().toString();

        handleInlineConditional(conditional.getThenExpr(), append(pathConds, cond), result);
        handleInlineConditional(conditional.getElseExpr(), append(pathConds, negate(cond)), result);
    }

    private static void handleYield(
        YieldStmt yield,
        List<String> pathConds,
        Map<String, String> result
    ) {
        String varName = yield.getExpression().toString().trim();
        String condition = joinConditions(pathConds);
        mergeCondition(result, varName, condition.isEmpty() ? "true" : condition);
    }

    private static void handleSwitchStmt(
        SwitchStmt sw,
        List<String> pathConds,
        Map<String, String> result
    ) {
        walkSwitchEntries(sw.getSelector().toString(), sw.getEntries(), pathConds, result);
    }

    private static void handleSwitchExpr(
        SwitchExpr sw,
        List<String> pathConds,
        Map<String, String> result
    ) {
        walkSwitchEntries(sw.getSelector().toString(), sw.getEntries(), pathConds, result);
    }

    private static void walkSwitchEntries(
        String selector,
        List<SwitchEntry> entries,
        List<String> pathConds,
        Map<String, String> result
    ) {
        // Collect every non-default label up front for building default negations
        List<String> nonDefaultLabels = entries.stream()
                .flatMap(e -> e.getLabels().stream())
                .map(Expression::toString)
                .toList();

        for (SwitchEntry entry : entries) {
            List<String> entryConds;

            if (entry.getLabels().isEmpty()) {
                // "default" / "default ->" clauses
                List<String> negs = nonDefaultLabels.stream()
                        .map(l -> selector + " != " + l)
                        .collect(Collectors.toList());
                entryConds = append(pathConds, joinConditions(negs));
            } else {
                // One or more comma-separated labels (arrow style) or stacked case: lines
                List<String> labelConds = entry.getLabels().stream()
                        .map(l -> selector + " == " + l)
                        .collect(Collectors.toList());
                String labelCond = labelConds.size() == 1
                        ? labelConds.get(0)
                        : "(" + String.join(" || ", labelConds) + ")";
                entryConds = append(pathConds, labelCond);
            }

            // Arrow entries with a direct expression (no braces) appear as an
            // ExpressionStmt wrapping the value — treat that as an implicit yield.
            if (isArrowExpressionEntry(entry)) {
                String varName = entry.getStatements().get(0)
                        .asExpressionStmt().getExpression().toString().trim();
                String condition = joinConditions(entryConds);
                mergeCondition(result, varName, condition.isEmpty() ? "true" : condition);
            } else {
                walkBlock(entry.getStatements(), entryConds, result);
            }
        }
    }

    // Returns true when this is an arrow-case entry whose body is a single
    // bare expression (not a block and not a throw/yield statement).
    // e.g. case 1 -> x;
    private static boolean isArrowExpressionEntry(SwitchEntry entry) {
        if (entry.getType() != SwitchEntry.Type.EXPRESSION) return false;
        return entry.getStatements().size() == 1
                && entry.getStatements().get(0).isExpressionStmt();
    }

    private static void handleIf(
        IfStmt ifStmt,
        List<String> pathConds,
        Map<String, String> result
    ) {
        String cond = ifStmt.getCondition().toString();

        List<String> thenConds = append(pathConds, cond);
        walkStatement(ifStmt.getThenStmt(), thenConds, result);

        ifStmt.getElseStmt().ifPresent(elseStmt -> {
            List<String> elseConds = append(pathConds, negate(cond));
            walkStatement(elseStmt, elseConds, result);
        });
    }

    // parses a boolean, Trigger, or BooleanSupplier, and returns the negation of that value.
    private static String negate(String cond) {
        cond = cond.trim();
        if (cond.startsWith("!(") && cond.endsWith(")")) return cond.substring(2, cond.length() - 1);
        if (cond.endsWith(".negate()")) return cond.substring(0, cond.length() - 9);
        if (cond.contains(" == ")) return cond.replace(" == ", " != ");
        if (cond.contains(" != ")) return cond.replace(" != ", " == ");
        if (cond.contains(" >= ")) return cond.replace(" >= ", " < ");
        if (cond.contains(" <= ")) return cond.replace(" <= ", " > ");
        if (cond.contains(" > "))  return cond.replace(" > ",  " <= ");
        if (cond.contains(" < "))  return cond.replace(" < ",  " >= ");

        boolean hasLogicalOps = cond.contains("&&") || cond.contains("||") || cond.contains(".and(") || cond.contains(".or(");
        if (hasLogicalOps) return "!(" + cond + ")";
        if (cond.startsWith("!") && !cond.startsWith("!(")) {
            return cond.substring(1);
        }
        return "!" + cond;
    }

    private static List<String> append(List<String> conds, String newCond) {
        if (newCond == null || newCond.isEmpty()) return conds;
        List<String> copy = new ArrayList<>(conds);
        copy.add(newCond);
        return copy;
    }

    private static String joinConditions(List<String> conds) {
        if (conds.isEmpty()) return "";
        return String.join(" && ", conds);
    }

    private static void mergeCondition(Map<String, String> result, String varName, String newCond) {
        result.merge(varName, newCond, (existing, incoming) ->
                "(" + existing + ") || (" + incoming + ")");
    }
}
