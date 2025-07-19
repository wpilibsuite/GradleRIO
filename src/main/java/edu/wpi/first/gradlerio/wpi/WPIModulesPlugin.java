package edu.wpi.first.gradlerio.wpi;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Configures java compilation tasks to automatically make available every module detected on the classpath.
 */
public class WPIModulesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getTasks().withType(JavaCompile.class).forEach(compileJava -> {
      compileJava.doFirst((task) -> {
        // Find all modular JAR files on the classpath, extract their module names (which may be different from
        // the names of the containing JARs!), and pass those module names to the compile task using the --add-modules
        // command line flag.
        //
        // --module-path is added as well so the compiler knows where to find those modules
        //
        // This is compatible with the modularity.inferModulePath setting that users can apply themselves (and which
        // defaults to `true` in modern versions of Gradle), and is also compatible with user programs that manually
        // specify a module-info.java file.  We primarily expect our users not to write a module-info file, though,
        // but still want to allow them to use `import module` statements. Thus, this plugin.
        //
        // See: https://dev.java/learn/modules/add-modules-reads/
        var moduleFinder =
            ModuleFinder.of(compileJava.getClasspath().getFiles().stream().map(File::toPath).toArray(Path[]::new));
        var moduleNames =
            moduleFinder.findAll().stream().map(mod -> mod.descriptor().name()).collect(Collectors.joining(","));

        var compilerArgs = new ArrayList<>(compileJava.getOptions().getCompilerArgs());
        compilerArgs.add("--module-path");
        compilerArgs.add(compileJava.getClasspath().getAsPath());

        compilerArgs.add("--add-modules");
        compilerArgs.add(moduleNames);

        project.getLogger().debug("Adding modules to the compile task `{}`:", compileJava.getName());
        moduleFinder.findAll().stream().sorted(Comparator.comparing(mod -> mod.descriptor().name())).forEach(mod -> {
          project.getLogger().debug("Adding module {} from {}", mod.descriptor().name(), mod.location().map(URI::toString).orElse("<unknown location>"));
        });

        compileJava.getOptions().setCompilerArgs(compilerArgs);
      });
    });
  }
}
