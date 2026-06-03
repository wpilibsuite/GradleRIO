
package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A gradle task that generates state machine names
 */
@DisableCachingByDefault
public abstract class MakeStateMachineGraphsTask extends DefaultTask {
    private record Transition(String fromState, String toState, String transitionCond) {}
    private static class StateMachineGraph {
        List<Transition> transitions = new ArrayList<>();
        String initialState;
        List<String> stateDefinitionOrder = new ArrayList<>();
    }

    private final Pattern unusedVarPattern = Pattern.compile("(?<=[(,\\s])_(?=[\\s,)->])");

    @Input
    @Optional
    public abstract Property<String> getJavaRoot();

    @Input
    @Optional
    public abstract Property<String> getDeployDirectory();

    @TaskAction
    public void run() throws IOException {
        extractFromDirectory(
            getJavaRoot().getOrElse("src/main/java/"),
            getDeployDirectory().getOrElse("src/main/deploy/")
        );
    }

    private void extractFromDirectory(String javaRoot, String deployDir) throws IOException {
        var graphs = new LinkedHashMap<String, StateMachineGraph>();
        // Init configuration for javaparser
        var config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        config.setPreprocessUnicodeEscapes(true);
        var parser = new JavaParser(config);

        Files.walk(Paths.get(javaRoot))
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(path -> {
                try {
                    extractFromFile(parser, path.toFile(), graphs);
                } catch (IOException e) {
                    System.err.println("Failed to parse: " + path + " — " + e.getMessage());
                }
            });

        for (var entry: graphs.entrySet()) {
            var methodName = entry.getKey();
            var graph = entry.getValue();
            var file = new File(deployDir, "/stateMachineGraphs/" + methodName + ".mermaid");
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (var writer = new FileWriter(file)) {
                writer.write(generateMermaid(graph));
            }
        }
    }

    private void extractFromFile(
        JavaParser parser,
        File sourceFile,
        Map<String, StateMachineGraph> graphs
    ) throws IOException {
        var content = Files.readString(sourceFile.toPath());
        // JavaParser 3.28.1 (and earlier) has issues with unnamed variables (JEP 456).
        // We replace them with a valid identifier before parsing.
        content = unusedVarPattern
            .matcher(content)
            .replaceAll(res -> "unused_var_" + UUID.randomUUID().toString().substring(0, 8));

        var cu = parser.parse(content).getResult().orElseThrow();
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            var annotationOpt = method.getAnnotationByName("MakeStateMachineGraph");
            if (annotationOpt.isEmpty()) return;
            var extractedType = "StateMachine";
            if (annotationOpt.get().isNormalAnnotationExpr()) {
                var data = annotationOpt.get()
                    .asNormalAnnotationExpr()
                    .getPairs()
                    .stream()
                    .filter(pair -> pair.getNameAsString().equals("stateMachineType"))
                    .findFirst();
                if (data.isPresent()) {
                    var valueExpr = data.get().getValue();
                    if (valueExpr.isStringLiteralExpr()) {
                        extractedType = valueExpr.asStringLiteralExpr().getValue();
                    }
                }
            }
            final var stateMachineType = extractedType;

            if (!method.getTypeAsString().equals(stateMachineType)) {
                throw new RuntimeException("Method " + method.getNameAsString() + " is annotated with @MakeStateMachineGraph, but it does not return " + stateMachineType);
            }

            var variableDefs = method.findAll(VariableDeclarationExpr.class);
            var stateSupplierTypes = List.of("Supplier<" + stateMachineType + ".State>", "Supplier<State>");
            boolean hasStateSupplierVar = variableDefs.stream()
                .filter(dec -> dec.getVariables().size() == 1)
                .anyMatch(dec -> stateSupplierTypes.contains(dec.getVariable(0).getTypeAsString()));
            if (hasStateSupplierVar) {
                throw new RuntimeException(
                    "Methods annotated with @MakeStateMachineGraph cannot contain variables of type Supplier<" + stateMachineType + ".State>."
                );
            }
            var stateMachineDefs = variableDefs
                .stream()
                .filter(v -> {
                    // we cannot check the type itself, because of var declarations -
                    // so, we check for the instantiation of the StateMachine class
                    var def = v.getVariable(0).getInitializer();
                    return def.isPresent() &&
                        def.get().isObjectCreationExpr() &&
                        def.get().asObjectCreationExpr().getType().asString().endsWith(stateMachineType);
                })
                .toList();
            if (stateMachineDefs.isEmpty()) {
                throw new RuntimeException("No " + stateMachineType + " declaration found in " + method.getNameAsString());
            } else if (stateMachineDefs.size() > 1) {
                throw new RuntimeException(
                    "Multiple " + stateMachineType + " declarations found in " + method.getNameAsString()
                    + ". Currently, the @MakeStateMachineGraph annotation doesn't support multiple state machine declarations in the same method."
                );
            }

            var stateMachineDef = stateMachineDefs.getFirst().getVariable(0).getInitializer().orElseThrow();
            var rawGraphName = stateMachineDef.asObjectCreationExpr().getArgument(0);
            if (!rawGraphName.isStringLiteralExpr()) {
                throw new RuntimeException(
                    "The graph generator requires the name " +
                    "of the state machine in '" + method.getNameAsString() +
                    "' to be a simple string literal (e.g. new " + stateMachineType + "(\"My Auto\"))."
                );
            }
            var graphName = rawGraphName.toString().substring(1, rawGraphName.toString().length() - 1);
            if (graphs.containsKey(graphName)) {
                throw new RuntimeException("2 state machines are named '" + graphName + "'");
            }
            var graph = new StateMachineGraph();
            graphs.put(graphName, graph);

            variableDefs.stream()
                .filter(v -> {
                    var initializer = v.getVariable(0).getInitializer();
                    return initializer.isPresent() &&
                            initializer.get().isMethodCallExpr() &&
                            initializer.get().asMethodCallExpr().getNameAsString().equals("addState");
                })
                .forEachOrdered(v -> graph.stateDefinitionOrder.add(v.getVariable(0).getNameAsString()));

            method.findAll(MethodCallExpr.class).forEach(call -> {
                if (call.getNameAsString().equals("setInitialState")) {
                    graph.initialState = call.getArguments().getFirst().orElseThrow().toString();
                    return;
                }

                boolean isWhen = call.getNameAsString().equals("when");
                boolean isWhenComplete = call.getNameAsString().equals("whenComplete");
                boolean isWhenCompleteAnd = call.getNameAsString().equals("whenCompleteAnd");
                if (!isWhen && !isWhenComplete && !isWhenCompleteAnd) return;

                var toStateCallOpt = call.getScope()
                        .filter(s -> s instanceof MethodCallExpr)
                        .map(s -> ((MethodCallExpr) s));
                if (toStateCallOpt.isEmpty()) return;

                var transitionCondExpr = call.getArguments().getFirst();
                var transitionCond = "";
                if (isWhenComplete) {
                    transitionCond = "when complete";
                } else {
                    var expr = transitionCondExpr.orElseThrow().toString();
                    expr = expr.replace("() -> ", "");
                    expr = expr.replace(".getAsBoolean()", "");
                    transitionCond = isWhen ? expr : Utils.joinWithAnd("when complete", expr);
                }

                var toStateCall = toStateCallOpt.get();
                var toStateExpr = toStateCall.getArguments().getFirst();
                var toState = toStateExpr.map(Expression::toString).orElse("Exit_State_Machine");
                boolean toStateIsLambda = toStateExpr.isPresent() && toStateExpr.get().isLambdaExpr();
                if (List.of("switchTo", "exitStateMachine").contains(toStateCall.getNameAsString())) {
                    var fromState = toStateCall.getScope().orElseThrow().toString();
                    if (toStateIsLambda) {
                        graph.transitions.addAll(
                            transitionsFromLambdaExpr(toStateExpr.get(), fromState, transitionCond)
                        );
                    } else {
                        graph.transitions.add(new Transition(fromState, toState, transitionCond));
                    }
                } else if (List.of("to", "toExitStateMachine").contains(toStateCall.getNameAsString())) {
                    // If it's just a regular "to", there must be a switchFromAny before it.
                    var argList = toStateCall.getScope()
                            .filter(s -> s instanceof MethodCallExpr)
                            .map(s -> ((MethodCallExpr) s))
                            .filter(s -> s.getNameAsString().equals("switchFromAny"))
                            .map(MethodCallExpr::getArguments);
                    if (argList.isEmpty()) return;
                    for (var fromState: argList.get()) {
                        if (toStateIsLambda) {
                            graph.transitions.addAll(
                                transitionsFromLambdaExpr(toStateExpr.get(), fromState.toString(), transitionCond)
                            );
                        } else {
                            graph.transitions.add(new Transition(fromState.toString(), toState, transitionCond));
                        }
                    }
                }
            });
        });
    }

    private List<Transition> transitionsFromLambdaExpr(
        Expression toState,
        String fromState,
        String transitionCond
    ) {
        var transitions = new ArrayList<Transition>();
        var body = toState.asLambdaExpr().getBody();
        var returnConditions = body.isBlockStmt()
            ? CodeBlockAnalyzer.analyze(body.asBlockStmt())
            : ExpressionAnalyzer.analyze(body.asExpressionStmt().getExpression());
        for (var entry: returnConditions.entrySet()) {
            var innerToState = entry.getKey();
            var additionalCond = entry.getValue();
            var fullCond = Utils.joinWithAnd(transitionCond, additionalCond);
            transitions.add(new Transition(fromState, innerToState, fullCond));
        }
        return transitions;
    }

    private String generateMermaid(StateMachineGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("state_definition_order: ");
        sb.append(graph.stateDefinitionOrder);
        sb.append("\n---\n");
        sb.append("stateDiagram-v2\n");
        sb.append("    direction LR\n\n");
        for (var t : graph.transitions) {
            sb.append("    ")
                .append(sanitize(t.fromState()))
                .append(" --> ")
                .append(sanitize(t.toState()))
                .append(" : ")
                .append(sanitizeTransitionCond(t.transitionCond()))
                .append("\n");
        }
        if (graph.initialState != null) {
            sb.append("\n    classDef initialState color: #00FF00");
            sb.append("\n    class ");
            sb.append(graph.initialState);
            sb.append(" initialState\n");
        }
        return sb.toString();
    }

    private String sanitizeTransitionCond(String condition) {
        condition = sanitize(condition);
        if (condition.isEmpty() || condition.equals("true")) {
            condition = "instant";
        } else if (condition.equals("false")) {
            condition = "never";
        }
        return condition;
    }

    private String sanitize(String s) {
        return s.replaceAll("[\"'\\n\\r]", "").trim();
    }
}
