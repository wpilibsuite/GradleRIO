package org.wpilib.gradlerio.wpi;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Configures java compilation tasks to automatically make available every module detected on the classpath. JAR files
 * with an Automatic-Module-Name manifest entry will be included, but JAR files with neither a module-info entry nor a
 * fallback manifest entry will <i>not</i> be added to the module path.
 *
 * <p>This basically does the same thing as org.gradle.internal.jvm.JavaModuleDetector, but where we don't care if the
 * project is modular. Gradle, by contrast, will only move modular dependencies to the module path if inferModulePath
 * is on <i>and</i> the project has a module-info file. This plugin is compatible with inferModulePath and modular
 * projects; both Gradle and the plugin will remove modular JARs from the classpath and place them on the module path,
 * and since they do the same thing we always get identical --module-path arguments passed to the compiler.
 */
public class WPIModulesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getTasks().withType(JavaCompile.class).configureEach(compileJava -> {
      FileCollection originalClasspath = compileJava.getClasspath();
      if (originalClasspath == null) {
        // Probably a custom task that hasn't had a classpath configured.
        // Do nothing and bail
        return;
      }

      // Split the original classpath into modular and nonmodular dependencies. This ensures dependencies only appear
      // on either the classpath or module path, never both. Note that any entries added to the classpath after the
      // plugin is applied will remain on the classpath, even if they are modular!
      FileCollection modular = originalClasspath.filter(f -> isModuleLike(f, project));
      FileCollection nonmodular = originalClasspath.filter(f -> !isModuleLike(f, project));
      compileJava.setClasspath(nonmodular);

      compileJava.getOptions().getCompilerArgumentProviders().add(() -> {
        if (modular.isEmpty()) {
          // No modular JARs - which is weird, since wpilib JARs should all be modular.
          // Exit early and do nothing instead of breaking the build.
          project.getLogger().warn(
              "No modular JARs were detected on the classpath for task '{}'. Are the WPILib dependencies present?",
              compileJava.getName());
          return List.of();
        }

        var moduleFinder =
            ModuleFinder.of(modular.getFiles().stream().map(File::toPath).toArray(Path[]::new));

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

        // Note: Gradle's DefaultJavaCompileSpec will search for an explicit --module-path argument and duplicate it.
        // There doesn't seem to be a good reason for this, but it causes our module path to appear twice in the
        // compiler arguments. This behavior won't cause problems because the last --module-path argument overrides any
        // earlier ones, so the work that Gradle is doing is merely redundant rather than harmful.
        return List.of(
            "--module-path",
            modular.getAsPath(),
            "--add-modules",
            moduleNames
        );
      });
    });
  }

  private boolean isModuleLike(File file, Project project) {
    if (file.isDirectory()) {
      // Loose directories need a module-info.class entry
      return new File(file, "module-info.class").isFile();
    }

    if (!file.getName().endsWith(".jar")) {
      // Given a file, but not a JAR.
      return false;
    }

    try (var jarFile = new JarFile(file)) {
      var moduleInfoEntry = jarFile.getJarEntry("module-info.class");
      if (moduleInfoEntry != null) {
        // This JAR file has explicit module information; add it to the module path.
        // Note that this automatically handles multi-release JARs, so if a dependency has module info
        // at META-INF/versions/9/module-info.class instead of in the root, it will still be detected
        project.getLogger().debug("Found module-info.class in dependency {}", file);
        return true;
      }

      // No explicit module information is present.
      // Fall back to scan for an Automatic-Module-Name manifest entry
      var manifest = jarFile.getManifest();
      var automaticName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
      if (automaticName != null && !automaticName.isEmpty()) {
        project.getLogger().debug(
            "Found Automatic-Module-Name manifest entry '{}' in dependency {}",
            automaticName, file);
        return true;
      }

      // No explicit module-info and no Automatic-Module-Name manifest entry.
      // This is not a modular dependency and will left on the classpath.
      project.getLogger().debug(
          "Did not find any module information in dependency {} - it will be left on the classpath", file);
      return false;
    } catch (IOException e) {
      throw new RuntimeException("Unable to examine classpath JAR " + file, e);
    }
  }
}
