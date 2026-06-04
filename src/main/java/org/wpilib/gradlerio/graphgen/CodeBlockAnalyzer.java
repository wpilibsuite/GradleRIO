package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;
import java.util.stream.Collectors;

class CodeBlockAnalyzer {
    /**
     * Analyzes a code block for returned state machine states,
     * and the conditions needed for the state to be returned.
     * @param block The code block to analyze
     * @return A map of state names to the conditions needed for that state to be returned
     */
    static Map<String, String> analyze(BlockStmt block) {
        var rawReturns = new HashMap<String, List<String>>();
        analyzeStatement(block, "true", rawReturns);
        return rawReturns.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Utils.joinWithOr(entry.getValue())
            ));
    }

    private static List<String> analyzeStatement(
        Statement stmt,
        String pathCondition,
        Map<String, List<String>> returnsMap
    ) {
        var fallThroughConditions = new ArrayList<String>();

        if (stmt.isBlockStmt()) {
            var currentPaths = Collections.singletonList(pathCondition);
            for (var subStmt: stmt.asBlockStmt().getStatements()) {
                var nextPaths = new ArrayList<String>();
                for (var path: currentPaths) {
                    nextPaths.addAll(analyzeStatement(subStmt, path, returnsMap));
                }
                currentPaths = nextPaths;
                if (currentPaths.isEmpty()) {
                    break; // Block always returns before hitting subsequent lines
                }
            }
            fallThroughConditions.addAll(currentPaths);
        } else if (stmt.isReturnStmt()) {
            var returnExpr = stmt.asReturnStmt().getExpression().orElseThrow();
            ExpressionAnalyzer.analyze(returnExpr, pathCondition, returnsMap);
        } else if (stmt.isIfStmt()) {
            var ifStmt = stmt.asIfStmt();
            var conditionStr = ifStmt.getCondition().toString();
            var thenCondition = Utils.joinWithAnd(pathCondition, conditionStr);
            var elseCondition = Utils.joinWithAnd(pathCondition, Utils.negate(conditionStr));

            fallThroughConditions.addAll(analyzeStatement(ifStmt.getThenStmt(), thenCondition, returnsMap));
            if (ifStmt.getElseStmt().isPresent()) {
                fallThroughConditions.addAll(analyzeStatement(ifStmt.getElseStmt().get(), elseCondition, returnsMap));
            } else {
                fallThroughConditions.add(elseCondition);
            }
        } else if (stmt.isSwitchStmt()) {
            var switchStmt = stmt.asSwitchStmt();
            var selector = switchStmt.getSelector().toString();
            var switchExitPaths = new ArrayList<String>();
            var pendingFallThrough = new ArrayList<String>();
            var selectorMatches = new ArrayList<String>();

            for (var entry: switchStmt.getEntries()) {
                List<String> labels = entry.getLabels().stream().map(Node::toString).toList();
                String entryDirectCondition;

                if (labels.isEmpty()) {
                    // 'default' case handles whatever hasn't matched prior explicit conditions
                    if (selectorMatches.isEmpty()) {
                        entryDirectCondition = pathCondition;
                    } else {
                        var combinedMatches = Utils.joinWithOr(selectorMatches);
                        entryDirectCondition = Utils.joinWithAnd(pathCondition, Utils.negate(combinedMatches));
                    }
                } else {
                    var matchCondition = Utils.joinWithOr(
                        labels.stream().map(label -> selector + " == " + label).toList()
                    );
                    entryDirectCondition = Utils.joinWithAnd(pathCondition, matchCondition);
                    selectorMatches.add(matchCondition);
                }

                // Current case entry path context = Direct match path + pending fall-through paths from previous case
                var caseIncomingPaths = new ArrayList<String>();
                caseIncomingPaths.add(entryDirectCondition);
                caseIncomingPaths.addAll(pendingFallThrough);
                pendingFallThrough.clear(); // Consumed by this case entry

                // Process internal statement sequences within this switch branch
                var currentCasePaths = caseIncomingPaths;
                for (var subStmt: entry.getStatements()) {
                    if (subStmt.isBreakStmt()) {
                        // 'break' safely escapes the switch completely
                        switchExitPaths.addAll(currentCasePaths);
                        currentCasePaths = new ArrayList<>();
                        break;
                    }
                    var nextCasePaths = new ArrayList<String>();
                    for (var path: currentCasePaths) {
                        nextCasePaths.addAll(analyzeStatement(subStmt, path, returnsMap));
                    }
                    currentCasePaths = nextCasePaths;
                    if (currentCasePaths.isEmpty()) break;
                }

                // Anything still active cascades directly to the next case block
                pendingFallThrough.addAll(currentCasePaths);
            }

            // Clean up left over paths
            switchExitPaths.addAll(pendingFallThrough);

            // If no default branch existed, values not explicit in cases fall through past the switch block
            boolean hasDefault = switchStmt.getEntries().stream().anyMatch(e -> e.getLabels().isEmpty());
            if (!hasDefault && !selectorMatches.isEmpty()) {
                var combinedMatches = Utils.joinWithOr(selectorMatches);
                switchExitPaths.add(Utils.joinWithAnd(pathCondition, Utils.negate(combinedMatches)));
            }
            fallThroughConditions.addAll(switchExitPaths);
        } else {
            fallThroughConditions.add(pathCondition);
        }

        return fallThroughConditions;
    }
}