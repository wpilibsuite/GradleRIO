package org.wpilib.gradlerio.wpi;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Configures java compilation tasks to automatically make available every module detected on the classpath. Automatic
 * modules will not be included.
 */
public class WPIModulesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getTasks().withType(JavaCompile.class).forEach(compileJava -> {
      compileJava.doFirst(task -> {
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
        var classpathJars =
            compileJava.getClasspath().getFiles().stream()
                .map(File::toPath)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .sorted(Comparator.comparing(Path::toString))
                .filter(p -> isModuleLike(p, project))
                .toList();

        var moduleFinder =
            ModuleFinder.of(classpathJars.toArray(Path[]::new));

        var modules = moduleFinder.findAll();

        project.getLogger().debug("Adding modules to the compile task `{}`:", compileJava.getName());
        modules.forEach(mod -> {
          project.getLogger().debug(
              "Adding module {} from {}",
              mod.descriptor().name(),
              mod.location().map(URI::toString).orElse("<unknown location>"));
        });

        var moduleNames =
            modules.stream().map(mod -> mod.descriptor().name()).collect(Collectors.joining(","));

        compileJava.getOptions().getCompilerArgs().addAll(List.of(
            "--module-path",
            compileJava.getClasspath().getAsPath(),
            "--add-modules",
            moduleNames
        ));
      });
    });
  }

  private boolean isModuleLike(Path path, Project project) {
    try (var jarFile = new JarFile(path.toFile())) {
      var moduleInfoEntry = jarFile.getJarEntry("module-info.class");
      if (moduleInfoEntry != null) {
        // This JAR file has explicit module information; add it to the module path.
        // Note that this automatically handles multi-release JARs, so if a dependency has module info
        // at META-INF/versions/9/module-info.class instead of in the root, it will still be detected
        project.getLogger().debug("Found module-info.class in dependency {}", path);
        return true;
      }

      // No explicit module information is present.
      // Fall back to scan for an Automatic-Module-Name manifest entry
      var manifest = jarFile.getManifest();
      var automaticName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
      if (automaticName != null && !automaticName.isEmpty()) {
        project.getLogger().debug(
            "Found Automatic-Module-Name manifest entry '{}' in dependency {}",
            automaticName, path);
        return true;
      }

      // No explicit module-info and no Automatic-Module-Name manifest entry.
      // This is therefore not
      project.getLogger().debug(
          "Did not find any module information in dependency {} - it will be left on the classpath", path);
      return false;
    } catch (IOException e) {
      throw new RuntimeException("Unable to examine classpath JAR " + path, e);
    }
  }
}
