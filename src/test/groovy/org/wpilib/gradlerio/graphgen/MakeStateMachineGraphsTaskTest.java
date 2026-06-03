package org.wpilib.gradlerio.graphgen;

import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MakeStateMachineGraphsTaskTest {
    @TempDir
    Path tempDir;

    private TaskProvider<MakeStateMachineGraphsTask> taskProvider;
    private Path javaRoot;
    private Path outputDir;

    @BeforeEach
    void setup() throws IOException {
        var project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        taskProvider = project.getTasks().register("makeGraphs", MakeStateMachineGraphsTask.class);
        
        javaRoot = tempDir.resolve("src/main/java").toAbsolutePath();
        Files.createDirectories(javaRoot);

        var deployDir = tempDir.resolve("src/main/deploy").toAbsolutePath();
        outputDir = deployDir.resolve("stateMachineGraphs");

        // Use the same path format as default to avoid any normalization issues in the task
        taskProvider.configure(task -> {
            task.getJavaRoot().set(javaRoot.toString());
            task.getDeployDirectory().set(deployDir.toAbsolutePath().toString());
        });
    }

    @Test
    void testBasicStateMachine() throws IOException {
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
  
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine simpleAuto() {
                    var sm = new StateMachine("Simple Auto");
                    var state1 = sm.addState(cmd("State1"));
                    var state2 = sm.addState(cmd("State2"));
          
                    sm.setInitialState(state1);
          
                    state1.switchTo(state2).when(() -> true);
                    state2.exitStateMachine().whenComplete();
          
                    return sm;
                }
           
                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
           """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        taskProvider.get().run();

        Path mermaidFile = outputDir.resolve("Simple Auto.mermaid");
        assertTrue(Files.exists(mermaidFile), "Mermaid file should be generated");
        
        String mermaidContent = Files.readString(mermaidFile);
        String expected = """
        ---
        state_definition_order: [state1, state2]
        ---
        stateDiagram-v2
            direction LR
        
            state1 --> state2 : instant
            state2 --> Exit_State_Machine : when complete
        
            classDef initialState color: #00FF00
            class state1 initialState
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void testComplexTransitions() throws IOException {
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
  
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine complexAuto() {
                    var sm = new StateMachine("Complex Auto");
                    var s1 = sm.addState(cmd("S1"));
                    var s2 = sm.addState(cmd("S2"));
                    var s3 = sm.addState(cmd("S3"));
          
                    s1.switchTo(s2).whenCompleteAnd(() -> someCondition());
                    sm.switchFromAny(s1, s2).to(s3).when(() -> emergency());
                    sm.switchFromAny(s3).toExitStateMachine().when(() -> done());
          
                    return sm;
                }

                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
           """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        taskProvider.get().run();

        String mermaidContent = Files.readString(outputDir.resolve("Complex Auto.mermaid"));
        String expected = """
        ---
        state_definition_order: [s1, s2, s3]
        ---
        stateDiagram-v2
            direction LR

            s1 --> s2 : when complete && someCondition()
            s1 --> s3 : emergency()
            s2 --> s3 : emergency()
            s3 --> Exit_State_Machine : done()
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void testIfElseInLambdaTransition() throws IOException {
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
          
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine lambdaAuto() {
                    var sm = new StateMachine("Lambda Auto");
                    var start = sm.addState(cmd("Start"));
                    var left = sm.addState(cmd("Left"));
                    var right = sm.addState(cmd("Right"));
           
                    start.switchTo(cond -> {
                        if (cond) return left;
                        else return right;
                    }).when(() -> check());
           
                    return sm;
                }

                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
           """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        taskProvider.get().run();

        String mermaidContent = Files.readString(outputDir.resolve("Lambda Auto.mermaid"));
        String expected = """
        ---
        state_definition_order: [start, left, right]
        ---
        stateDiagram-v2
            direction LR

            start --> left : check() && cond
            start --> right : check() && !cond
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void testNestedLogicStatements() throws IOException {
        String content = """
            package frc.robot;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
            
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine doubleSM() {
                    var sm1 = new StateMachine("Early Return Auto");
                    var a = sm1.addState(cmd("a"));
                    var b = sm1.addState(cmd("b"));
                    var c = sm1.addState(cmd("c"));
            
                    a.switchTo(() -> {
                        switch (number) {
                            case 0, 1:
                                if (condition1) {
                                    if (condition2) {
                                        return c;
                                    }
                                }
                                return b;
                        }
                        return a;
                    }).whenComplete();
                    return sm1;
                }
            }
            """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        taskProvider.get().run();

        String mermaidContent = Files.readString(outputDir.resolve("Early Return Auto.mermaid"));
        String expected = """
        ---
        state_definition_order: [a, b, c]
        ---
        stateDiagram-v2
            direction LR
        
            a --> a : when complete && !(number == 0 || number == 1)
            a --> b : when complete && (((number == 0 || number == 1) && condition1) && !condition2) || ((number == 0 || number == 1) && !condition1)
            a --> c : when complete && (((number == 0 || number == 1) && condition1) && condition2)
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void testUnnamedVariables() throws IOException {
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
           
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine unnamedAuto() {
                    var sm = new StateMachine("Unnamed Auto");
                    var s1 = sm.addState(cmd("S1"));
                    var s2 = sm.addState(cmd("S2"));
          
                    s1.switchTo(_ -> s2).when(() -> true);
          
                    return sm;
                }

                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
           """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        // Should not throw exception despite '_' being invalid in some Java versions for identifiers
        assertDoesNotThrow(() -> taskProvider.get().run());

        String mermaidContent = Files.readString(outputDir.resolve("Unnamed Auto.mermaid"));
        String expected = """
        ---
        state_definition_order: [s1, s2]
        ---
        stateDiagram-v2
            direction LR

            s1 --> s2 : instant
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void testMultipleStateMachinesInMethodThrows() throws IOException {
        String content = """
            package frc.robot;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
            
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine doubleSM() {
                    var sm1 = new StateMachine("SM1");
                    var sm2 = new StateMachine("SM2");
                    return sm1;
                }
            }
            """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        assertThrows(RuntimeException.class, () -> taskProvider.get().run());
    }

    @Test
    void stateSupplierInStateMachineThrows() throws IOException {
        String content = """
            package frc.robot;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
            
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine doubleSM() {
                    var sm1 = new StateMachine("SM1");
                    var state1 = sm1.addState(cmd("state1"));
                    Supplier<StateMachine.State> derived = () -> state1;
                    return sm1;
                }
            
                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
            """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        assertThrows(RuntimeException.class, () -> taskProvider.get().run());
    }

    @Test
    void testCustomStateMachineType() throws IOException {
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
  
            public class Robot {
                @MakeStateMachineGraph(stateMachineType = "CustomStateMachine")
                public CustomStateMachine customAuto() {
                    var sm = new CustomStateMachine("Custom Auto");
                    var state1 = sm.addState(cmd("State1"));
                    var state2 = sm.addState(cmd("State2"));
          
                    sm.setInitialState(state1);
          
                    state1.switchTo(state2).when(() -> true);
                    state2.exitStateMachine().whenComplete();
          
                    return sm;
                }
           
                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
           """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        taskProvider.get().run();

        Path mermaidFile = outputDir.resolve("Custom Auto.mermaid");
        assertTrue(Files.exists(mermaidFile), "Mermaid file should be generated");
        
        String mermaidContent = Files.readString(mermaidFile);
        String expected = """
        ---
        state_definition_order: [state1, state2]
        ---
        stateDiagram-v2
            direction LR
        
            state1 --> state2 : instant
            state2 --> Exit_State_Machine : when complete
        
            classDef initialState color: #00FF00
            class state1 initialState
        """;
        assertEquals(expected.strip(), mermaidContent.strip());
    }

    @Test
    void stateSupplierInCustomStateMachineThrows() throws IOException {
        String content = """
            package frc.robot;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
            
            public class Robot {
                @MakeStateMachineGraph(stateMachineType = "CustomStateMachine")
                public CustomStateMachine doubleSM() {
                    var sm1 = new CustomStateMachine("SM1");
                    var state1 = sm1.addState(cmd("state1"));
                    Supplier<CustomStateMachine.State> derived = () -> state1;
                    return sm1;
                }
            
                private Command cmd(String name) {
                    return Command.noRequirements(coro -> while(true) coro.yield()).named(name);
                }
            }
            """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        var ex = assertThrows(RuntimeException.class, () -> taskProvider.get().run());
        assertTrue(ex.getMessage().contains("Supplier<CustomStateMachine.State>"), "Error message should mention Supplier<CustomStateMachine.State>");
    }
}