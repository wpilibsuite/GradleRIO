package edu.wpi.first.gradlerio.wpi.cpp;

import edu.wpi.first.nativeutils.NativeUtilsExtension;
import edu.wpi.first.toolchain.NativePlatforms;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.stream.Stream;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.language.base.internal.ProjectLayout;
import org.gradle.language.nativeplatform.tasks.AbstractNativeSourceCompileTask;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.nativeplatform.BuildTypeContainer;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.TargetedNativeComponent;
import org.gradle.nativeplatform.test.googletest.GoogleTestTestSuiteBinarySpec;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.ComponentSpec;
import org.gradle.platform.base.ComponentSpecContainer;
import org.gradle.platform.base.internal.BinarySpecInternal;

public class WPINativeCompileRules extends RuleSource {

  @Mutate
  public void addBuildTypes(BuildTypeContainer bts) {
    bts.maybeCreate("debug");
    bts.maybeCreate("release");
  }

  @Mutate
  public void addBinaryFlags(BinaryContainer binaries, ExtensionContainer extensions) {
    NativeUtilsExtension ntExt = extensions.getByType(NativeUtilsExtension.class);

    binaries.withType(
        NativeBinarySpec.class,
        bin -> {
          ntExt.usePlatformArguments(bin);
        });
  }

  @Mutate
  public void addBinaryFlags(BinaryContainer binaries) {
    binaries.withType(
        GoogleTestTestSuiteBinarySpec.class,
        bin -> {
          if (!bin.getTargetPlatform().getName().equals(NativePlatforms.desktop)) {
            ((BinarySpecInternal) bin).setBuildable(false);
          }
          bin.getCppCompiler().define("RUNNING_FRC_TESTS");
          bin.getcCompiler().define("RUNNING_FRC_TESTS");
        });
  }

  @Validate
  void setupCompilerWarningPrints(
      ModelMap<Task> tasks, ProjectLayout layout, ComponentSpecContainer components) {
    if (components == null) return;
    Project project = (Project) layout.getProjectIdentifier();

    for (ComponentSpec c : components) {
      if (c instanceof TargetedNativeComponent) {
        for (BinarySpec bin : ((TargetedNativeComponent) c).getBinaries()) {
          bin.getTasks()
              .withType(
                  AbstractNativeSourceCompileTask.class,
                  t -> {
                    t.doLast(
                        new Action<Task>() {
                          @Override
                          public void execute(Task arg0) {
                            printWarningsForBinTask(t.getName().toString(), project);
                          }
                        });
                  });
        }
      }
    }
  }

  // From
  // https://github.com/wpilibsuite/native-utils/blob/a8ea595670716c7b898878a37e36c2b52e8e3f42/src/main/groovy/edu/wpi/first/nativeutils/rules/BuildConfigRules.groovy#L450
  private static void printWarningsForBinTask(String taskName, Project project) {
    File file = new File(project.getBuildDir(), "tmp/" + taskName + "/output.txt");

    if (!file.exists()) return;

    String currentFile = "";
    boolean hasFirstLine = false;
    boolean hasPrintedFileName = false;

    try (Stream<String> rawIterator = Files.lines(file.toPath())) {
      Iterable<String> fileIterator =
          new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
              return rawIterator.iterator();
            }
          };

      for (String line : fileIterator) {
        if (!hasFirstLine) {
          hasFirstLine = true;
        } else if (line.startsWith("compiling ")) {
          currentFile = line.substring(10, line.indexOf("successful."));
          hasPrintedFileName = false;
        } else if (line.contains("Finished") && line.contains("see full log")) {
          // No op
        } else if (line.trim().equals(currentFile.trim())) {
          // No op
        } else if (!line.isEmpty()) {
          if (!hasPrintedFileName) {
            hasPrintedFileName = true;
            System.out.println("Warnings in file " + currentFile + "....");
          }
          System.out.println(line);
        }
      }
    } catch (IOException e) {
      Logging.getLogger(WPINativeCompileRules.class)
          .warn(
              "Failed to print warnings file. You might need to manually open it to find any compile warnings",
              e);
    }
  }
}
