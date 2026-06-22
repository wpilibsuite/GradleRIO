package org.wpilib.gradlerio.graphgen;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.google.gson.Gson;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A gradle task that generates diagrams of state machines written in Commands V3.
 */
@DisableCachingByDefault
public abstract class MakeStateMachineGraphsTask extends DefaultTask {
    private record Transition(String origin, String target, String condition) {}
    private static class StateMachineGraph {
        final List<Transition> transitions = new ArrayList<>();
        final List<String> stateDefinitionOrder = new ArrayList<>();
        String initialState = "";
    }

    private final Pattern unusedVarPattern = Pattern.compile("(?<=[(,\\s])_(?=[\\s,)->])");
    private final Gson gson = new Gson();

    @Input
    @Optional
    public abstract Property<String> getJavaRoot();

    @Input
    @Optional
    public abstract Property<String> getDeployDirectory();

    @Input
    @Optional
    public abstract Property<String> getDiagramGenerationDirectory();

    @TaskAction
    public void run() throws IOException {
        extractFromDirectory(
            getJavaRoot().getOrElse("src/main/java/"),
            getDeployDirectory().getOrElse("src/main/deploy/"),
            getDiagramGenerationDirectory().getOrElse("stateMachineGraphs/")
        );
    }

    private void extractFromDirectory(String javaRoot, String deployDir, String diagramGenDir) throws IOException {
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
                    ErrorLogger.setFilePath(path.toString().replace(javaRoot, ""));
                    extractFromFile(parser, path.toFile(), graphs);
                } catch (IOException e) {
                    System.err.println("Failed to parse: " + path + " — " + e.getMessage());
                }
            });

        new File(deployDir, "/stateMachineGraphData").mkdirs();
        new File(diagramGenDir).mkdirs();
        for (var entry: graphs.entrySet()) {
            var name = entry.getKey();
            var graph = entry.getValue();
            Files.writeString(
                new File(diagramGenDir, name + ".mermaid").toPath(),
                generateMermaid(graph)
            );
            Files.writeString(
                new File(deployDir, "/stateMachineGraphData/" + name + ".json").toPath(),
                gson.toJson(graph)
            );
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

        var cu = parser.parse(content).getResult();
        if (cu.isEmpty()) return;
        cu.get().findAll(MethodDeclaration.class).forEach(method -> {
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
                ErrorLogger.throwError("Method " + method.getNameAsString() + " is annotated with @MakeStateMachineGraph, but it does not return " + stateMachineType, method);
            }

            var variableDefs = method.findAll(VariableDeclarationExpr.class);
            var stateSupplierTypes = List.of("Supplier<" + stateMachineType + ".State>", "Supplier<State>");
            variableDefs.stream()
                .filter(dec -> dec.getVariables().size() == 1)
                .filter(dec -> stateSupplierTypes.contains(dec.getVariable(0).getTypeAsString()))
                .findFirst()
                .ifPresent(illegalSupplierVar -> ErrorLogger.throwError(
                    "Methods annotated with @MakeStateMachineGraph cannot contain variables of type Supplier<State>.",
                    illegalSupplierVar
                ));
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
                ErrorLogger.throwError("No " + stateMachineType + " declaration found in " + method.getNameAsString(), method);
            } else if (stateMachineDefs.size() > 1) {
                ErrorLogger.throwError(
                    "Multiple " + stateMachineType + " declarations found in " + method.getNameAsString()
                    + ". Currently, the @MakeStateMachineGraph annotation doesn't support multiple state machine declarations in the same method.",
                    stateMachineDefs.get(1)
                );
            }

            var stateMachineDef = stateMachineDefs.getFirst().getVariable(0).getInitializer().orElseThrow();
            var rawGraphName = stateMachineDef.asObjectCreationExpr().getArgument(0);
            if (!rawGraphName.isStringLiteralExpr()) {
                ErrorLogger.throwError(
                    "The graph generator requires the name " +
                    "of the state machine in '" + method.getNameAsString() +
                    "' to be a simple string literal (e.g. new " + stateMachineType + "(\"My Auto\")).",
                    stateMachineDef
                );
            }
            var graphName = rawGraphName.toString().substring(1, rawGraphName.toString().length() - 1);
            if (graphs.containsKey(graphName)) {
                ErrorLogger.throwError("There is already a state machine named '" + graphName + "'", stateMachineDef);
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
                    call.getArguments().getFirst().ifPresent(state -> graph.initialState = state.toString());
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
                    if (transitionCondExpr.isEmpty()) return;
                    var expr = transitionCondExpr.get().toString();
                    expr = expr.replace("() -> ", "");
                    expr = expr.replace(".getAsBoolean()", "");
                    transitionCond = isWhen ? expr : Utils.joinWithAnd("when complete", expr);
                }

                var toStateCall = toStateCallOpt.get();
                var toStateExpr = toStateCall.getArguments().getFirst();
                var toState = toStateExpr.map(Expression::toString).orElse("Exit_State_Machine");
                boolean toStateIsLambda = toStateExpr.isPresent() && toStateExpr.get().isLambdaExpr();

                if (toStateCall.getScope().isEmpty()) return;
                if (List.of("switchTo", "exitStateMachine").contains(toStateCall.getNameAsString())) {
                    var fromState = toStateCall.getScope().get().toString();
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
                .append(sanitize(t.origin()))
                .append(" --> ")
                .append(sanitize(t.target()))
                .append(" : ")
                .append(sanitizeTransitionCond(t.condition()))
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
