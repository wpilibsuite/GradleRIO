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
                    System.out.println("Processing simpleAuto");
                    StateMachine sm = new StateMachine("Simple Auto");
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
        assertTrue(mermaidContent.contains("state_definition_order: [state1, state2]"));
        assertTrue(mermaidContent.contains("state1 --> state2 : true"));
        assertTrue(mermaidContent.contains("state2 --> Exit_State_Machine : when complete"));
        assertTrue(mermaidContent.contains("class state1 initialState"));
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
                    StateMachine sm = new StateMachine("Complex Auto");
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
        System.out.println(mermaidContent);
        assertTrue(mermaidContent.contains("s1 --> s2 : when complete and someCondition()"));
        assertTrue(mermaidContent.contains("s1 --> s3 : emergency()"));
        assertTrue(mermaidContent.contains("s2 --> s3 : emergency()"));
        assertTrue(mermaidContent.contains("s3 --> Exit_State_Machine : done()"));
    }

    @Test
    void testLambdaTransitions() throws IOException {
        // This tests the interaction with ReturnConditionAnalyzer
        String content = """
            package frc.robot;
           
            import org.wpilib.command3.Command;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
          
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine lambdaAuto() {
                    StateMachine sm = new StateMachine("Lambda Auto");
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
        assertTrue(mermaidContent.contains("start --> left : check() and cond"));
        assertTrue(mermaidContent.contains("start --> right : check() and !cond"));
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
                    StateMachine sm = new StateMachine("Unnamed Auto");
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
        System.out.println(mermaidContent);
        assertTrue(mermaidContent.contains("s1 --> s2"));
    }

    @Test
    void testMultipleStateMachinesInMethodThrows() throws IOException {
        String content = """
            package frc.robot;
            import org.wpilib.gradlerio.graphgen.MakeStateMachineGraph;
            
            public class Robot {
                @MakeStateMachineGraph
                public StateMachine doubleSM() {
                    StateMachine sm1 = new StateMachine("SM1");
                    StateMachine sm2 = new StateMachine("SM2");
                    return sm1;
                }
            }
            """;
        Files.writeString(javaRoot.resolve("Robot.java"), content);

        assertThrows(RuntimeException.class, () -> taskProvider.get().run());
    }
}